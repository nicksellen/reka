package reka;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static reka.api.Path.slashes;
import static reka.config.configurer.Configurer.configure;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.admin.AdminUtils;
import reka.api.Path;
import reka.api.data.Data;
import reka.api.data.MutableData;
import reka.api.flow.Flow;
import reka.config.NavigableConfig;
import reka.config.Source;
import reka.config.parser.ConfigParser;
import reka.core.builder.FlowVisualizer;
import reka.core.data.memory.MutableMemoryData;
import reka.core.module.ModuleManager;
import reka.core.setup.ModuleStatusReport;
import reka.core.setup.StatusProvider;
import reka.dirs.AppDirs;
import reka.dirs.BaseDirs;

public class ApplicationManager implements Iterable<Entry<String,Application>> {
	
	private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	private final EventLogger eventLogger = new EventLogger("/tmp/rekalog");
	
	private final BaseDirs basedirs;
	private final ModuleManager moduleManager;
	
	private final ConcurrentMap<String,Application> applications = new ConcurrentHashMap<>();
	private final ConcurrentMap<String,AtomicInteger> versions = new ConcurrentHashMap<>();
	
	private final ConcurrentMap<String,List<ModuleStatusReport>> status = new ConcurrentHashMap<>();
	
	private final List<EventListener> listeners = Collections.synchronizedList(new ArrayList<>());
	
	private final BlockingDeque<ApplicationTask> q = new LinkedBlockingDeque<>();
	
	private final ApplicationTask UPDATE_STATUS = new UpdateStatus();

	public ApplicationManager(BaseDirs dirs, ModuleManager moduleManager) {
		this.basedirs = dirs;
		this.moduleManager = moduleManager;
		executor.submit(new WaitForNextTask());
		Reka.SCHEDULED_SERVICE.scheduleAtFixedRate(() -> q.push(UPDATE_STATUS), 1, 1, TimeUnit.SECONDS);
		emitSystemMessage("started");
	}
	
	private static interface ApplicationTask extends Runnable { }
	
	private class WaitForNextTask implements ApplicationTask {

		@Override
		public void run() {
			try {
				executor.submit(q.take());
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
	}
	
	private class UpdateStatus implements ApplicationTask {

		@Override
		public void run() {
			try {
				Map<String, List<ModuleStatusReport>> changed = new HashMap<>();
				applications.forEach((identity, app) -> {
					List<ModuleStatusReport> appStatus = reportsFor(app);
					List<ModuleStatusReport> previousAppStatus = status.put(identity, appStatus);
					if (previousAppStatus != null && !appStatus.equals(previousAppStatus)) {
						changed.put(identity, appStatus);
					}
				});
				if (!changed.isEmpty()) {
					notifyStatusListeners(changed);
				}
			} catch (Throwable t) {
				t.printStackTrace();
			} finally {
				executor.submit(new WaitForNextTask());
			}
		}
		
	}
	
	private class UndeployApplication implements ApplicationTask {
		
		private final String identity;
		
		public UndeployApplication(String identity) {
			this.identity = identity;
		}

		@Override
		public void run() {
			log.info("running undeploy task {}", identity);
			try {
				Application app = applications.remove(identity);
				versions.remove(identity);
				if (app == null) return;
				app.undeploy();
				status.remove(identity);
				log.info("undeployed [{}]", app.fullName());
				notifyUndeployListeners(identity, app);
			} finally {
				executor.submit(new WaitForNextTask());
			}
		}
		
	}
	
	private class DeployApplication implements ApplicationTask {

		private final String identity;
		private final int incomingVersion;
		private final NavigableConfig originalConfig;
		private final File constrainTo;
		private final DeploySubscriber subscriber;
		
		public DeployApplication(String identity, int version, NavigableConfig originalConfig, File constrainTo, DeploySubscriber subscriber) {
			this.identity = identity;
			this.incomingVersion = version;
			this.originalConfig = originalConfig;
			this.constrainTo = constrainTo != null ? constrainTo : new File("/");
			this.subscriber = subscriber;
		}

		@Override
		public void run() {
			int version = incomingVersion != -1 ? incomingVersion : nextVersion(identity);
			
			log.info("running deploy task {}", identity);

			Optional<Application> previous = Optional.ofNullable(applications.get(identity));
			
			try {
				
				checkArgument(constrainTo.isDirectory(), "constraint dir %s is not a dir", constrainTo.getAbsolutePath());
				
				log.info("deploying {} v{}", identity, version);
				
				NavigableConfig config = moduleManager.processor().process(originalConfig);
				
				AppDirs dirs = basedirs.resolve(identity, version);
				
				dirs.mkdirs();
				
				ApplicationConfigurer configurer = configure(new ApplicationConfigurer(dirs, moduleManager), config);
				
				configurer.checkValid(identity);
				
				previous.ifPresent(Application::pause);
				
				configurer.build(identity, version).whenComplete((app, ex) -> {
					try {
						if (app != null) {
							applications.put(identity, app);
							previous.ifPresent(Application::undeploy);
							List<ModuleStatusReport> reports = reportsFor(app);
							status.put(identity, reports);
							log.info("deployed [{}] listening on {}", app.fullName(), app.network().stream().map(Object::toString).collect(joining(", ")));
							notifyDeployListeners(identity, app, reports);
							subscriber.ok(identity, version, app);
							versions.putIfAbsent(identity, new AtomicInteger());
							versions.get(identity).set(version);
						} else if (ex != null) {
							log.info("exception whilst deploying!");
							subscriber.error(identity, ex);
							previous.ifPresent(Application::resume);
						}
					} finally {
						executor.submit(new WaitForNextTask());
					}
				});
				
			} catch (Throwable t) {
				subscriber.error(identity, t);
				previous.ifPresent(Application::resume);
				executor.submit(new WaitForNextTask());
			}
		}
		
	}
	
	private static List<ModuleStatusReport> reportsFor(Application app) {
		List<ModuleStatusReport> appStatus = app.statusProviders().stream().map(StatusProvider::report).collect(toList());
		appStatus.sort(comparing(ModuleStatusReport::name));
		return appStatus;
	}
	
	private static class EventListener {
		private final Flow flow;
		private final EnumSet<EventType> types;
		EventListener(Flow flow, EventType[] types) {
			this.flow = flow;
			this.types = EnumSet.copyOf(asList(types));
		}
	}
	
	public void addListener(Flow flow, EventType... eventTypes) {
		listeners.add(new EventListener(flow, eventTypes));
	}
	
	public void removeListener(Flow flow) {
		Iterator<EventListener> it = listeners.iterator();
		while (it.hasNext()) {
			if (it.next().flow.equals(flow)) {
				it.remove();
			}
		}
	}
	
	public void undeploy(String identity) {
		q.push(new UndeployApplication(identity));
	}

	private void notifyDeployListeners(String identity, Application app, List<ModuleStatusReport> reports) {
		emit(EventType.deploy, AdminUtils.putAppDetails(MutableMemoryData.create(), app, Optional.of(reports))
				.putString("id", identity));
	}
	
	private void notifyUndeployListeners(String identity, Application app) {
		emit(EventType.undeploy, MutableMemoryData.create()
				.putString("id", identity)
				.putInt("version", app.version())
				.putString("name", app.fullName()));
	}
	
	private void notifyStatusListeners(Map<String, List<ModuleStatusReport>> changed) {
		changed.forEach((identity, reports) -> {			
			emit(EventType.status, MutableMemoryData.create()
				.putString("id", identity)
				.putList("status", list -> {
					reports.forEach(report -> {
						list.add(report.data());
					});
				}));
		});
	}
	
	public static enum EventType {
		status, deploy, undeploy, system;
	}
	
	private void emit(EventType type, MutableData incomingData) {

		// TODO: perhaps delegate this eventLogger business to a db...
		
		long eid = eventLogger.nextEventId();
		Data data = incomingData.putLong("eid", eid).immutable();

		//log.info("emitting event {}: {}", type, data);
		eventLogger.write(eid, type, data);
		
		listeners.forEach(listener -> {
			if (listener.types.contains(type)) {
				listener.flow.prepare().data(MutableMemoryData.from(data)).stats(false).run();
			}
		});
	}
	
	private void emitSystemMessage(String message) {
		emit(EventType.system, MutableMemoryData.create().putString("message", message));
	}

	public void validate(String identity, Source source) {
		NavigableConfig config = moduleManager.processor().process(ConfigParser.fromSource(source));
		configure(new ApplicationConfigurer(basedirs.mktemp(), moduleManager), config).checkValid(identity);
	}
	
	public static interface DeploySubscriber {
		
		public static final DeploySubscriber LOG = new DeploySubscriber(){
			
			private final Logger log = LoggerFactory.getLogger(getClass());

			@Override
			public void ok(String identity, int version, Application application) {
				log.info("deployed {}", identity);
			}

			@Override
			public void error(String identity, Throwable t) {
				log.error(format("failed to deploy %s", identity), t);
			}
			
		};
		
		void ok(String identity, int version, Application application);
		void error(String identity, Throwable t);
	}
	
	public void deployConfig(String identity, int version, NavigableConfig config, File constrainTo, DeploySubscriber subscriber) {
		q.push(new DeployApplication(identity, version, config, constrainTo, subscriber));
	}
	
	public void deploySource(String identity, int version, Source source, DeploySubscriber subscriber) {
		File constrainTo = new File("/");
		if (source.isFile()) constrainTo = source.file().getParentFile();
		deployConfig(identity, version, ConfigParser.fromSource(source), constrainTo, subscriber);
	}
	
	public static final Path INITIALIZER_VISUALIZER_NAME = slashes("app/initialize");
	
	public Optional<FlowVisualizer> visualize(String identity, Path flowName) {
		Application app = applications.get(identity);
		if (app == null) return Optional.empty();
		
		if (INITIALIZER_VISUALIZER_NAME.equals(flowName)) {
			return Optional.of(app.initializerVisualizer());
		}
		
		return Optional.ofNullable(app.flows().visualizer(flowName));
	}
	
	public Collection<FlowVisualizer> visualize(NavigableConfig config) {
		return configure(new ApplicationConfigurer(basedirs.mktemp(), moduleManager), config).visualize();
	}
	
	public Optional<Application> get(String identity) {
		return Optional.ofNullable(applications.get(identity));
	}
	
	public Optional<List<ModuleStatusReport>> statusFor(String identity) {
		return Optional.ofNullable(status.get(identity));
	}
	
	public int version(String identity) {
		AtomicInteger v = versions.get(identity);
		return v != null ? v.get() : -1;
	}
	
	public int nextVersion(String identity) {
		return version(identity) + 1;
	}

	@Override
	public Iterator<Entry<String,Application>> iterator() {
		return applications.entrySet().iterator();
	}

	public void shutdown() {
		emitSystemMessage("shutting down");
		applications.values().forEach(app -> app.undeploy());
		emitSystemMessage("shutdown complete");
	}
	
}
