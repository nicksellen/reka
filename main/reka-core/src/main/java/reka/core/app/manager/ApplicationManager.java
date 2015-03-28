package reka.core.app.manager;

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
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.EventLogger;
import reka.Identity;
import reka.Reka;
import reka.admin.AdminUtils;
import reka.api.ConcurrentIdentityStore;
import reka.api.IdentityStore;
import reka.api.Path;
import reka.api.data.MutableData;
import reka.api.flow.Flow;
import reka.config.NavigableConfig;
import reka.config.Source;
import reka.config.parser.ConfigParser;
import reka.core.app.Application;
import reka.core.app.ApplicationConfigurer;
import reka.core.app.IdentityAndVersion;
import reka.core.builder.FlowVisualizer;
import reka.core.data.memory.MutableMemoryData;
import reka.core.module.ModuleManager;
import reka.core.setup.ModuleStatusReport;
import reka.core.setup.StatusProvider;
import reka.dirs.AppDirs;
import reka.dirs.BaseDirs;
import reka.util.AsyncShutdown;

public class ApplicationManager implements Iterable<Entry<Identity,Application>>, AsyncShutdown {

	public static enum EventType {
		
		status, deploy, undeploy, system;
		
		private final String val;
		
		EventType() {
			this.val = format("reka:%s", toString());
		}
		
		public String val() {
			return val;
		}
		
	}
	
	public static interface DeploySubscriber {
		
		public static final DeploySubscriber LOG = new DeploySubscriber(){
			
			private final Logger log = LoggerFactory.getLogger(getClass());

			@Override
			public void ok(Identity identity, int version, Application application) {
				log.info("deployed {}", identity);
			}

			@Override
			public void error(Identity identity, Throwable t) {
				log.error(format("failed to deploy %s", identity), t);
			}
			
		};
		
		void ok(Identity identity, int version, Application application);
		void error(Identity identity, Throwable t);
	}
	
	private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	private final EventLogger eventLogger = new EventLogger("/tmp/rekalog");
	
	private final BaseDirs basedirs;
	private final ModuleManager moduleManager;
	
	private final ConcurrentMap<Path,Identity> identities = new ConcurrentHashMap<>();
	private final ConcurrentMap<Identity,Application> applications = new ConcurrentHashMap<>();
	private final ConcurrentMap<Identity,AtomicInteger> versions = new ConcurrentHashMap<>();
	
	private final ConcurrentMap<Identity,List<ModuleStatusReport>> status = new ConcurrentHashMap<>();
	
	private final List<EventListener> listeners = Collections.synchronizedList(new ArrayList<>());
	
	private final BlockingDeque<AsyncApplicationTask> q = new LinkedBlockingDeque<>();
	
	private final AsyncApplicationTask UPDATE_STATUS = new UpdateStatus().async();
	private final ScheduledFuture<?> scheduledStatus;

	public ApplicationManager(BaseDirs dirs, ModuleManager moduleManager) {
		this.basedirs = dirs;
		this.moduleManager = moduleManager;
		executor.submit(new WaitForNextTask());
		scheduledStatus = Reka.SharedExecutors.scheduled.scheduleAtFixedRate(() -> q.push(UPDATE_STATUS), 1, 1, TimeUnit.SECONDS);
		emitSystemMessage("started");
	}
	
	public void deployConfig(Path path, int version, NavigableConfig config, File constrainTo, DeploySubscriber subscriber) {
		q.push(new DeployApplication(path, version, config, constrainTo, subscriber));
	}
	
	public void deploySource(Path path, int version, Source source, DeploySubscriber subscriber) {
		File constrainTo = new File("/");
		if (source.isFile()) constrainTo = source.file().getParentFile();
		deployConfig(path, version, ConfigParser.fromSource(source), constrainTo, subscriber);
	}
	
	public void undeploy(Path path) {
		q.push(new UndeployApplication(path));
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

	public void validate(Source source) {
		Identity identity = Identity.create("validate");
		IdentityAndVersion idv = IdentityAndVersion.create(identity, versions.get(identity).get());
		NavigableConfig config = moduleManager.processor().process(ConfigParser.fromSource(source));
		configure(new ApplicationConfigurer(basedirs.mktemp(), moduleManager), config).checkValid(idv);
	}
	
	public static final Path INITIALIZER_VISUALIZER_NAME = slashes("app/initialize");
	
	public Optional<FlowVisualizer> visualize(Identity identity, Path flowName) {
		Application app = applications.get(identity);
		if (app == null) return Optional.empty();
		
		if (INITIALIZER_VISUALIZER_NAME.equals(flowName)) {
			return Optional.of(app.initializerVisualizer());
		}
		
		return Optional.ofNullable(app.flows().visualizer(flowName));
	}
	
	public Collection<FlowVisualizer> visualize(NavigableConfig config) {
		return configure(new ApplicationConfigurer(basedirs.mktemp(), moduleManager), config).visualize(IdentityAndVersion.tmp());
	}
	
	public Optional<Application> get(Identity identity) {
		return Optional.ofNullable(applications.get(identity));
	}
	
	public Optional<List<ModuleStatusReport>> statusFor(Identity identity) {
		return Optional.ofNullable(status.get(identity));
	}
	
	public int version(Identity identity) {
		AtomicInteger v = versions.get(identity);
		return v != null ? v.get() : 0;
	}

	public Identity identityFor(Path path) {
		return identities.get(path);
	}
	
	public int nextVersion(Path appPath) {
		Identity identity = identityFor(appPath);
		return identity != null ? version(identity) + 1 : 1;
	}
	
	public int nextVersion(Identity identity) {
		return version(identity) + 1;
	}

	@Override
	public Iterator<Entry<Identity,Application>> iterator() {
		return applications.entrySet().iterator();
	}
	
	private class UpdateStatus implements ApplicationTask {

		@Override
		public void run() {
			Map<Identity, List<ModuleStatusReport>> changed = new HashMap<>();
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
		}
		
	}
	
	private class UndeployApplication implements AsyncApplicationTask {
		
		private final Path path;
		
		public UndeployApplication(Path path) {
			this.path = path;
		}

		@Override
		public void run(TaskResult res) {
			Identity identity = identityFor(path);
			if (identity == null) {
				res.complete();
				return;
			}
			Application app = applications.get(identity);
			if (app == null) {
				res.complete();
				return;
			};
			app.shutdown(new Result(){

				@Override
				public void complete() {
					status.remove(identity);
					versions.remove(identity);
					applications.remove(identity);
					log.info("undeployed [{}]", app.fullName());
					notifyUndeployListeners(identity, app);
					res.complete();
				}

				@Override
				public void completeExceptionally(Throwable t) {
					// should I do anything here?
					res.completeExceptionally(t);
				}
				
			});
		}
		
	}
	
	private class DeployApplication implements AsyncApplicationTask {

		private final Path appPath;
		private final int incomingVersion;
		private final NavigableConfig originalConfig;
		private final File constrainTo;
		private final DeploySubscriber subscriber;
		
		public DeployApplication(Path appPath, int version, NavigableConfig originalConfig, File constrainTo, DeploySubscriber subscriber) {
			this.appPath = appPath;
			this.incomingVersion = version;
			this.originalConfig = originalConfig;
			this.constrainTo = constrainTo != null ? constrainTo : new File("/");
			this.subscriber = subscriber;
		}

		@Override
		public void run(TaskResult res) {
						
			Identity identity = identities.computeIfAbsent(appPath, unused -> Identity.create(appPath.slashes()));
			
			int version = incomingVersion > 0 ? incomingVersion : nextVersion(identity);

			Optional<Application> previous = Optional.ofNullable(applications.get(identity));
			
			AtomicReference<Runnable> unpause = new AtomicReference<>();
			
			Consumer<Throwable> onError = t -> {
				log.error("exception whilst deploying!");
				subscriber.error(identity, t);
				previous.ifPresent(Application::resume);
				res.completeExceptionally(t);
			};
			
			try {
				
				checkArgument(constrainTo.isDirectory(), "constraint dir %s is not a dir", constrainTo.getAbsolutePath());
				
				IdentityAndVersion idv = IdentityAndVersion.create(identity, version);
				log.info("deploying {} v{}", identity, version);
				
				NavigableConfig config = moduleManager.processor().process(originalConfig);
				
				AppDirs dirs = basedirs.resolve(appPath, version);
				
				dirs.mkdirs();
				
				ApplicationConfigurer configurer = configure(new ApplicationConfigurer(dirs, moduleManager), config);
				
				configurer.checkValid(idv);
				
				previous.ifPresent(p -> {
					unpause.set(p.pause());
				});
				
				IdentityStore store = previous.isPresent() ? ConcurrentIdentityStore.createFrom(previous.get().store()) : ConcurrentIdentityStore.create();
				
				configurer.build(identity, version, store).whenComplete((app, t) -> {
					try {
						if (app != null) {
							applications.put(identity, app);
							previous.ifPresent(Application::undeploy);
							List<ModuleStatusReport> reports = reportsFor(app);
							status.put(identity, reports);
							log.info("deployed [{}] listening on {}", app.fullName(), app.network().stream().map(Object::toString).collect(joining(", ")));
							notifyDeployListeners(identity, app, reports);
							subscriber.ok(identity, version, app);
							versions.putIfAbsent(identity, new AtomicInteger(1));
							versions.get(identity).set(version);
						} else if (t != null) {
							onError.accept(t);
						}
					} finally {
						Runnable r = unpause.get();
						if (r != null) {
							r.run();
						}
						res.complete();
					}
				});
				
			} catch (Throwable t) {
				onError.accept(t);
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

	private void notifyDeployListeners(Identity identity, Application app, List<ModuleStatusReport> reports) {
		emit(EventType.deploy, AdminUtils.putAppDetails(MutableMemoryData.create(), app, Optional.of(reports))
				.putString("id", identity.name()));
	}
	
	private void notifyUndeployListeners(Identity identity, Application app) {
		emit(EventType.undeploy, MutableMemoryData.create()
				.putString("id", identity.name())
				.putInt("version", app.version())
				.putString("name", app.fullName()));
	}
	
	private void notifyStatusListeners(Map<Identity, List<ModuleStatusReport>> changed) {
		changed.forEach((identity, reports) -> {			
			emit(EventType.status, MutableMemoryData.create()
				.putString("id", identity.name())
				.putList("status", list -> {
					reports.forEach(report -> {
						list.add(report.data());
					});
				}));
		});
	}
	
	private void emit(EventType type, MutableData incomingData) {
		long eid = eventLogger.write(type.val, incomingData);
		incomingData.putLong("eid", eid);
		listeners.forEach(listener -> {
			if (listener.types.contains(type)) {
				listener.flow.prepare().mutableData(MutableMemoryData.from(incomingData)).stats(false).run();
			}
		});
		
	}
	
	private void emitSystemMessage(String message) {
		emit(EventType.system, MutableMemoryData.create().putString("message", message));
	}


	@Override
	public void shutdown(AsyncShutdown.Result res) {
		emitSystemMessage("shutting down manager");
		AsyncShutdown.shutdownAll(applications.values(), new AsyncShutdown.Result() {

			@Override
			public void complete() {
				scheduledStatus.cancel(true);
				emitSystemMessage("manager shutdown complete");
				res.complete();
			}
			
			@Override
			public void completeExceptionally(Throwable t) {
				scheduledStatus.cancel(true);
				emitSystemMessage("manager shutdown complete with error");
				res.completeExceptionally(t);
			}
			
		});
	}
	
	private static interface TaskResult {
		void complete();
		void completeExceptionally(Throwable t);
	}
	
	private static interface ApplicationTask {
		void run() throws Exception;
		default AsyncApplicationTask async() {
			return new AsyncApplicationTask(){

				@Override
				public void run(TaskResult res) throws Exception {
					ApplicationTask.this.run();
					res.complete();
				}
				
			};
		}
	}
	
	private static interface AsyncApplicationTask {
		void run(TaskResult res) throws Exception;
	}
	
	private class WaitForNextTask implements Runnable {

		@Override
		public void run() {
			try {
				submit(q.take());
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
	}
	
	private void submit(AsyncApplicationTask task) {
		executor.submit(() -> {
			AtomicBoolean done = new AtomicBoolean(false);
			try {
				task.run(new TaskResult() {

					
					@Override
					public void complete() {
						if (done.compareAndSet(false, true)) {
							executor.submit(new WaitForNextTask());
						}
					}
					
					@Override
					public void completeExceptionally(Throwable t) {
						if (done.compareAndSet(false, true)) {
							executor.submit(new WaitForNextTask());
						}
					}
					
				});
			} catch (Throwable t) {
				if (done.compareAndSet(false, true)) {
					executor.submit(new WaitForNextTask());
				}
			}
		});
	}
	
}
