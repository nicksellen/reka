package reka.core.bundle;

import static java.util.stream.Collectors.toList;
import static reka.config.configurer.Configurer.configure;
import static reka.core.builder.FlowSegments.async;
import static reka.core.builder.FlowSegments.label;
import static reka.core.builder.FlowSegments.par;
import static reka.core.builder.FlowSegments.seq;
import static reka.core.builder.FlowSegments.sync;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import reka.DeployedResource;
import reka.SimpleDeployedResource;
import reka.api.Path;
import reka.api.data.Data;
import reka.api.flow.Flow;
import reka.api.flow.FlowSegment;
import reka.api.run.AsyncOperation;
import reka.api.run.SyncOperation;
import reka.config.ConfigBody;
import reka.core.builder.FlowSegments;
import reka.core.config.ConfigurerProvider;
import reka.core.config.SequenceConfigurer;

public class ModuleSetup {
	
	public static interface ModuleExecutor extends Supplier<FlowSegment> {
		ModuleExecutor run(String name, SyncOperation operation);		
		ModuleExecutor runAsync(String name, AsyncOperation operation);
		ModuleExecutor sequential(Consumer<ModuleExecutor> seq);
		ModuleExecutor sequential(String label, Consumer<ModuleExecutor> seq);
		ModuleExecutor parallel(Consumer<ModuleExecutor> par);
		ModuleExecutor parallel(String label, Consumer<ModuleExecutor> par);
	}
	
	private abstract static class AbstractExecutor implements ModuleExecutor {
		
		private final List<Supplier<FlowSegment>> segments = new ArrayList<>();

		@Override
		public ModuleExecutor run(String name, SyncOperation operation) {
			segments.add(() -> sync(name, () -> operation));
			return this;
		}

		@Override
		public ModuleExecutor runAsync(String name, AsyncOperation operation) {
			segments.add(() -> async(name, () -> operation));
			return this;
		}

		@Override
		public ModuleExecutor sequential(Consumer<ModuleExecutor> seq) {
			ModuleExecutor e = new SequentialExecutor();
			seq.accept(e);
			segments.add(e);
			return this;
		}

		@Override
		public ModuleExecutor sequential(String label, Consumer<ModuleExecutor> seq) {
			ModuleExecutor e = new SequentialExecutor();
			seq.accept(e);
			segments.add(() -> FlowSegments.label(label, e.get()));
			return this;
		}

		@Override
		public ModuleExecutor parallel(Consumer<ModuleExecutor> par) {
			ModuleExecutor e = new ParallelExecutor();
			par.accept(e);
			segments.add(e);
			return this;
		}
		

		@Override
		public ModuleExecutor parallel(String label, Consumer<ModuleExecutor> par) {
			ModuleExecutor e = new ParallelExecutor();
			par.accept(e);
			segments.add(() -> FlowSegments.label(label, e.get()));
			return this;
		}

		@Override
		public FlowSegment get() {
			List<FlowSegment> built = segments.stream().map(Supplier<FlowSegment>::get).collect(toList());
			return build(built);
		}
		
		abstract FlowSegment build(Collection<FlowSegment> segments);
		
	}
	
	private static class SequentialExecutor extends AbstractExecutor {

		@Override
		FlowSegment build(Collection<FlowSegment> segments) {
			return FlowSegments.seq(segments);
		}
		
	}

	
	private static class ParallelExecutor extends AbstractExecutor {

		@Override
		FlowSegment build(Collection<FlowSegment> segments) {
			return par(segments);
		}
		
	}

	private final Path path;
	private final List<Supplier<FlowSegment>> segments = new ArrayList<>();
	private final List<Runnable> shutdownHandlers = new ArrayList<>();
	private final List<TriggerCollection> triggers = new ArrayList<>();
	private final Map<Path,Function<ConfigurerProvider,Supplier<FlowSegment>>> providers = new HashMap<>();
	
	public ModuleSetup(Path path) {
		this.path = path;
	}
	
	public Path path() {
		return path;
	}
	
	public ModuleSetup init(String name, SyncOperation operation) {
		segments.add(() -> sync(name, () -> operation));
		return this;
	}
	
	public ModuleSetup initParallel(Consumer<ModuleExecutor> parallel) {
		ModuleExecutor e = new ParallelExecutor();
		parallel.accept(e);
		segments.add(e);
		return this;
	}
	
	public ModuleSetup initAsync(String name, AsyncOperation operation) {
		segments.add(() -> async(name, () -> operation));
		return this;
	}

	public void shutdown(String name, Runnable handler) {
		shutdownHandlers.add(handler);
	}
	
	public ModuleSetup operation(Path name, Supplier<Supplier<FlowSegment>> supplier) {
		providers.put(path.add(name), (provider) -> supplier.get());
		return this;
	}
	
	public ModuleSetup operation(Path name, Function<ConfigurerProvider,Supplier<FlowSegment>> provider) {
		providers.put(path.add(name), provider);
		return this;
	}

	public ModuleSetup operation(Iterable<Path> names, Supplier<Supplier<FlowSegment>> supplier) {
		Function<ConfigurerProvider,Supplier<FlowSegment>> provider = (p) -> supplier.get();
		for (Path name : names) {
			operation(name, provider);
		}
		return this;
	}

	public ModuleSetup operation(Iterable<Path> names, Function<ConfigurerProvider,Supplier<FlowSegment>> provider) {
		for (Path name : names) {
			operation(name, provider);
		}
		return this;
	}

	public static abstract class BaseRegistration {

		private final int applicationVersion;
		private final String identity;

		private final List<DeployedResource> resources = new ArrayList<>();
		private final List<PortAndProtocol> network = new ArrayList<>();
		
		public BaseRegistration(int applicationVersion, String identity) {
			this.applicationVersion = applicationVersion;
			this.identity = identity;
		}

		public int applicationVersion() {
			return applicationVersion;
		}
		
		public String applicationIdentity() {
			return identity;
		}
		
		public void resource(DeployedResource r) {
			resources.add(r);
		}

		public void undeploy(Runnable r) {
			resource(new SimpleDeployedResource() {
				@Override
				public void undeploy(int version) {
					r.run();
				}
			});
		}
		
		public void network(int port, String protocol, Data details) {
			network.add(new PortAndProtocol(port, protocol, details));
		}
		
		public List<DeployedResource> resources() {
			return resources;
		}
		
		public List<PortAndProtocol> network() {
			return network;
		}
		
	}
	
	public static class MultiRegistration extends BaseRegistration {
		
		private final Map<String,Flow> map;
		
		public MultiRegistration(int applicationVersion, String identity, Map<String,Flow> map) {
			super(applicationVersion, identity);
			this.map = map;
		}

		public boolean has(String name) {
			return map.containsKey(name);
		}
		
		public Flow get(String name) {
			return map.get(name);
		}
		
		public SingleRegistration singleFor(String name) {
			return new SingleRegistration(applicationVersion(), name, get(name));
		}
		
	}
	
	public static class SingleRegistration extends BaseRegistration {

		private final Flow flow;
		
		public SingleRegistration(int applicationVersion, String identity, Flow flow) {
			super(applicationVersion, identity);
			this.flow = flow;
		}
		
		public Flow flow() {
			return flow;
		}
		
	}
	
	public static class TriggerCollection {
		
		private final List<Trigger> triggers;
		private final Consumer<MultiRegistration> consumer;
		
		public TriggerCollection(Collection<Trigger> triggers, Consumer<MultiRegistration> consumer) {
			this.triggers = new ArrayList<>(triggers);
			this.consumer = consumer;
		}
		
		public List<Trigger> get() {
			return triggers;
		}
		
		public Consumer<MultiRegistration> consumer() {
			return consumer;
		}
		
	}
	
	public static class Trigger {
		
		private final String name;
		private final Function<ConfigurerProvider, Supplier<FlowSegment>> supplier;
		
		public Trigger(String name, Function<ConfigurerProvider, Supplier<FlowSegment>> supplier) {
			this.name = name;
			this.supplier = supplier;
		}
		
		public String name() {
			return name;
		}
		
		public Function<ConfigurerProvider, Supplier<FlowSegment>> supplier() {
			return supplier;
		}
		
	}
	
	public ModuleSetup trigger(String name, ConfigBody body, Consumer<SingleRegistration> c) {
		return trigger(name,  (provider) -> configure(new SequenceConfigurer(provider), body), c);
	}
	
	public ModuleSetup trigger(String name, Function<ConfigurerProvider, Supplier<FlowSegment>> supplier, Consumer<SingleRegistration> c) {
		Map<String,Function<ConfigurerProvider, Supplier<FlowSegment>>> suppliers = new HashMap<>();
		suppliers.put(name, supplier);
		return triggers(suppliers, m -> c.accept(m.singleFor(name)));
	}
	
	public ModuleSetup triggers(Map<String,Function<ConfigurerProvider, Supplier<FlowSegment>>> suppliers, Consumer<MultiRegistration> cs) {
		triggers.add(new TriggerCollection(suppliers.entrySet().stream().map(e -> new Trigger(e.getKey(), e.getValue())).collect(toList()), cs));
		return this;
	}
	
	public Optional<FlowSegment> buildFlowSegment() {
		if (segments.isEmpty()) return Optional.empty();
		List<FlowSegment> built = segments.stream().map(Supplier<FlowSegment>::get).collect(toList());
		return Optional.of(label(path.slashes(), seq(built)));
	}
	
	public Map<Path,Function<ConfigurerProvider,Supplier<FlowSegment>>> providers() {
		return providers;
	}
	
	public List<TriggerCollection> triggers() {
		return triggers;
	}
	
	public List<Runnable> shutdownHandlers() {
		return shutdownHandlers;
	}
	
}