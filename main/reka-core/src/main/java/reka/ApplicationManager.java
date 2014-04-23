package reka;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.stream.Collectors.joining;
import static reka.configurer.Configurer.configure;
import static reka.util.Util.runtime;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.StampedLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.api.Path;
import reka.api.run.EverythingSubscriber;
import reka.api.run.Subscriber;
import reka.config.FileSource;
import reka.config.NavigableConfig;
import reka.config.Source;
import reka.config.parser.ConfigParser;
import reka.core.builder.FlowVisualizer;
import reka.core.bundle.BundleManager;

import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;

public class ApplicationManager implements Iterable<Entry<String,Application>> {
	
	private final ExecutorService executor = Executors.newSingleThreadExecutor();
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	private final StampedLock lock = new StampedLock();
	
	private final java.nio.file.Path stateFilePath;
	
	private final BundleManager bundles;
	
	private final Map<String,Application> applications = new HashMap<>();
	private final Map<String,Source> applicationSource = new HashMap<>();
	private final Map<String,AtomicInteger> versions = new HashMap<>();
	
	private final Set<String> deployedFilenames = new HashSet<>();
	
	public ApplicationManager(File datadir, BundleManager bundles) {
		this.bundles = bundles;
		stateFilePath = datadir.toPath().resolve(".reka").toAbsolutePath();
	}
	
	public void restore() {
		if (!stateFilePath.toFile().exists()) return;
		
		List<String> lines;
		try {
			lines = Files.readAllLines(stateFilePath);
		} catch (IOException e) {
			log.error("failed to read lines");
			return;
		}
		
		for (String entry : lines) {
			try {
				Iterator<String> s = Splitter.on(":").limit(2).split(entry).iterator();
				String identity = s.next();
				String filename = s.next();
				if (deployedFilenames.contains(filename)) continue;
				log.info("restoring {} {}", identity, filename);
				deploy(identity, FileSource.from(new File(filename)));
			} catch (Throwable t) {
				log.error("failed to restore state", t);
			}
		}
		
	}
	
	public void undeploy(String identity) {
		
		long stamp = lock.writeLock();
		
		try {
			Application app = applications.remove(identity);
			Source source = applicationSource.remove(identity);
			versions.remove(identity);
			if (app == null) return;
			app.undeploy();
			log.info("undeployed [{}]", app.fullName());
			if (source.isFile()) {
				deployedFilenames.remove(source.file().getAbsolutePath());
			}
			saveState();
		} finally {
			lock.unlock(stamp);
		}
	}

	public void redeploy(String identity) {
		redeploy(identity, EverythingSubscriber.DO_NOTHING);
	}
	
	public void redeploy(String identity, Subscriber subscriber) {
		Source source = applicationSource.get(identity);
		checkNotNull(source, "we don't have the source for %s", identity);
		deploy(identity, true, source, false, subscriber);
	}

	public void deploy(String identity, Source source) {
		deploy(identity, false, source, false, EverythingSubscriber.DO_NOTHING);
	}

	public void deployTransient(String identity, Source source) {
		deploy(identity, false, source, true, EverythingSubscriber.DO_NOTHING);
	}
	
	public void deploy(String identity, Source source, Subscriber subscriber) {
		deploy(identity, false, source, false, subscriber);
	}
	
	public void deploy(String identity, boolean redeploy, Source source, boolean isTransient, Subscriber subscriber) {
		
		long stamp = lock.writeLock();
		
		try {
		
			if (source.isFile()) {
				String filename = source.file().getAbsolutePath();
				if (deployedFilenames.contains(filename) && !redeploy) {
					throw runtime("we already have a deployment of %s", filename);
				}
			}
				
			Application previous = applications.remove(identity);
			
			executor.execute(() -> {
				
				log.info("deploying {}{}", source, isTransient ? " (transient)" : "");
				
				NavigableConfig config = bundles.processor().process(ConfigParser.fromSource(source));
		
				versions.putIfAbsent(identity, new AtomicInteger());
				int version = versions.get(identity).incrementAndGet();

				if (previous != null) {
					// TODO: how do I get atomic redeploys again?
					// it's complicated because it might be some services need to be closed, and reopened.
					// I kind of need something to say: 
					//   "hold any incoming requests wherever they come from, we have a new app on the way"
					// but only where the service details actually match...
					previous.undeploy();
				}
			
				configure(new ApplicationConfigurer(bundles), config)
					.build(identity, version, previous, subscriber)
					.whenComplete((app, ex) -> {
					try {
						if (app != null) {
							applications.put(identity, app);
							if (!isTransient) {
								applicationSource.put(identity, source);
								if (source.isFile()) {
									deployedFilenames.add(source.file().getAbsolutePath());
								}
							}
							log.info("deployed [{}] listening on {}", app.fullName(), app.ports().stream().map(Object::toString).collect(joining(", ")));
						} else if (ex != null) {
							ex.printStackTrace();
						}
						saveState();
					} finally {
						lock.unlock(stamp);
					}
					
				});
				
			});
		} catch (Throwable t) {
			lock.unlock(stamp);
			throw t;
		}
	}
	public Optional<FlowVisualizer> visualize(String identity, Path flowName) {
		long stamp = lock.readLock();
		try {
			Application app = applications.get(identity);
			if (app == null) return Optional.empty();
			return Optional.ofNullable(app.flows().visualizer(flowName));
		} finally {
			lock.unlock(stamp);
		}
	}
	
	public Collection<FlowVisualizer> visualize(NavigableConfig config) {
		long stamp = lock.readLock();
		try {
			return configure(new ApplicationConfigurer(bundles), config).visualize();
		} finally {
			lock.unlock(stamp);
		}
	}
	
	public Optional<Application> get(String identity) {
		long stamp = lock.readLock();
		try {
			return Optional.ofNullable(applications.get(identity));
		} finally {
			lock.unlock(stamp);
		}
	}
	
	public int version(String identity) {
		long stamp = lock.readLock();
		try {
			AtomicInteger v = versions.get(identity);
			return v != null ? v.get() : -1;
		} finally {
			lock.unlock(stamp);
		}
	}

	@Override
	public Iterator<Entry<String,Application>> iterator() {
		long stamp = lock.readLock();
		try {
			return ImmutableMap.copyOf(applications).entrySet().iterator();
		} finally {
			lock.unlock(stamp);
		}
	}

	private void saveState() {
		try {
			StringBuilder content = new StringBuilder();
			for (Entry<String, Source> entry : applicationSource.entrySet()) {
				String id = entry.getKey();
				Source source = entry.getValue();
				if (source.isFile()) {
					content.append(id).append(':').append(source.file().getAbsolutePath()).append('\n');
				}
			}
			Files.write(stateFilePath, content.toString().getBytes(Charsets.UTF_8));
		} catch (Throwable t) {
			log.error("failed to save state", t);
		}
	}
	
}
