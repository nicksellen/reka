package reka.core.setup;

import static java.util.stream.Collectors.toList;
import static reka.config.configurer.Configurer.configure;
import static reka.core.builder.FlowSegments.createLabelSegment;
import static reka.core.builder.FlowSegments.seq;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

import reka.api.IdentityKey;
import reka.api.IdentityStore;
import reka.api.Path;
import reka.api.data.Data;
import reka.api.data.MutableData;
import reka.api.flow.Flow;
import reka.api.flow.FlowSegment;
import reka.api.run.AsyncOperation;
import reka.api.run.Operation;
import reka.config.Config;
import reka.config.ConfigBody;
import reka.core.bundle.BundleConfigurer.ModuleInfo;
import reka.core.config.ConfigurerProvider;
import reka.core.config.SequenceConfigurer;
import reka.core.setup.ModuleConfigurer.ModuleCollector;

public class ModuleSetup {
	
	private final ModuleInfo info;
	private final ModuleCollector collector;
	private final Path path;
	private final IdentityStore store;
	private final List<Supplier<FlowSegment>> segments = new ArrayList<>();	
	
	private boolean includeDefaultStatus = true;
	
	public ModuleSetup(ModuleInfo info, Path path, IdentityStore store, ModuleCollector collector) {
		this.info = info;
		this.path = path;
		this.store = store;
		this.collector = collector;
		if (info != null) {
			collector.versions.put(info.type(), info.version());
		}
	}
	
	protected boolean includeDefaultStatus() {
		return includeDefaultStatus;
	}
	
	public Path path() {
		return path;
	}
	
	public static interface DoneCallback extends Runnable {
		void done();
		default void run() {
			done();
		}
	}
	
	public static class InitFlowSetup {
		
		private final Flow flow;
		private final IdentityStore store;
		
		public InitFlowSetup(Flow flow, IdentityStore store) {
			this.flow = flow;
			this.store = store;
		}
		
		public Flow flow() {
			return flow;
		}
		
		public IdentityStore store() {
			return store;
		}
	}
	
	public ModuleSetup setupInitializer(Consumer<ModuleOperationSetup> init) {
		OperationSetup e = new SequentialCollector(path, store);
		init.accept(new ModuleOperationSetup(e));
		segments.add(e);
		return this;
	}
	
	// wraps the full-on OperationSetup and only allows direct operations to be defined, which just see the store
	public static class ModuleOperationSetup {
		
		private final OperationSetup ops;
		
		public ModuleOperationSetup(OperationSetup ops) {
			this.ops = ops;
		}
		
		public ModuleOperationSetup run(String name, Consumer<IdentityStore> c) {
			ops.add(name, store -> {
				return new Operation() {
					
					@Override
					public void call(MutableData data) {
						c.accept(store);
					}
					
				};
			});
			return this;
		}
		
		public ModuleOperationSetup runAsync(String name, BiConsumer<IdentityStore, DoneCallback> c) {
			ops.add(name, store -> {
				return AsyncOperation.create((data, ctx) -> c.accept(store, () -> ctx.done()));
			});
			return this;
		}
		
		public ModuleOperationSetup parallel(Consumer<ModuleOperationSetup> par) {
			ops.parallel(p -> {
				par.accept(new ModuleOperationSetup(p));
			});
			return this;
		}
		
		public <T> ModuleOperationSetup eachParallel(Iterable<T> it, BiConsumer<T, ModuleOperationSetup> seq) {
			ops.eachParallel(it, (v, s) -> {
				seq.accept(v, new ModuleOperationSetup(s));
			});
			return this;
		}
		
	}
	
	public void shutdown(String name, Consumer<IdentityStore> handler) {
		collector.shutdownHandlers.add(() -> handler.accept(store));
	}
	
	public ModuleSetup operation(Path name, Function<ConfigurerProvider,OperationConfigurer> c) {
		collector.providers.put(path.add(name), (provider, config) -> {
			return configure(c.apply(provider), config).bind(path, store);
		});
		return this;
	}
	
	public static interface FlowSegmentBiFunction extends BiFunction<ConfigurerProvider, Config, Supplier<FlowSegment>> {
	}

	private static abstract class BaseRegistration {

		private final int applicationVersion;
		private final String identity;
		private final IdentityStore store;

		private final List<NetworkInfo> network;
		
		private final List<IntConsumer> undeployConsumers;
		private final List<IntConsumer> pauseConsumers;
		private final List<IntConsumer> resumeConsumers;
		
		public BaseRegistration(
				int applicationVersion, 
				String identity,
				IdentityStore store,
				List<NetworkInfo> network,
				List<IntConsumer> undeployConsumers,
				List<IntConsumer> pauseConsumers,
				List<IntConsumer> resumeConsumers) {
			this.applicationVersion = applicationVersion;
			this.identity = identity;
			this.store = store;
			this.network = network;
			this.undeployConsumers = undeployConsumers;
			this.pauseConsumers = pauseConsumers;
			this.resumeConsumers = resumeConsumers;
		}

		public int applicationVersion() {
			return applicationVersion;
		}
		
		public String applicationIdentity() {
			return identity;
		}
		
		public IdentityStore store() {
			return store;
		}
		
		public void undeploy(IntConsumer c) {
			undeployConsumers.add(c);
		}
		
		public void pause(IntConsumer c) {
			pauseConsumers.add(c);
		}
		
		public void resume(IntConsumer c) {
			resumeConsumers.add	(c);
		}
		
		public List<IntConsumer> undeployConsumers() {
			return undeployConsumers;
		}
		
		public List<IntConsumer> pauseConsumers() {
			return pauseConsumers;
		}

		public List<IntConsumer> resumeConsumers() {
			return resumeConsumers;
		}
		
		public void network(int port, String protocol, Data details) {
			network.add(new NetworkInfo(port, protocol, details));
		}
		
		public List<NetworkInfo> network() {
			return network;
		}
		
	}
	
	public static class MultiFlowRegistration extends BaseRegistration {
		
		private final Map<IdentityKey<Flow>,Flow> map;
		
		public MultiFlowRegistration(int applicationVersion, String identity, IdentityStore store, Map<IdentityKey<Flow>,Flow> map) {
			super(applicationVersion, identity, store, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
			this.map = map;
		}

		public boolean has(IdentityKey<Flow> name) {
			return map.containsKey(name);
		}
		
		public Flow get(IdentityKey<Flow> name) {
			return map.get(name);
		}
		
		public SingleFlowRegistration singleFor(IdentityKey<Flow> name) {
			return new SingleFlowRegistration(this, get(name));
		}
		
	}
	
	public static class SingleFlowRegistration extends BaseRegistration {

		private final Flow flow;
		
		private SingleFlowRegistration(BaseRegistration base, Flow flow) {
			super(base.applicationVersion, base.identity, base.store, base.network, base.undeployConsumers, base.pauseConsumers, base.resumeConsumers);
			this.flow = flow;
		}
		
		public Flow flow() {
			return flow;
		}
		
	}
	
	public static class TriggerCollection {
		
		private final List<Trigger> triggers;
		private final Consumer<MultiFlowRegistration> consumer;
		private final IdentityStore store;
		
		public TriggerCollection(Collection<Trigger> triggers, Consumer<MultiFlowRegistration> consumer, IdentityStore store) {
			this.triggers = new ArrayList<>(triggers);
			this.consumer = consumer;
			this.store = store;
		}
		
		public List<Trigger> get() {
			return triggers;
		}
		
		public Consumer<MultiFlowRegistration> consumer() {
			return consumer;
		}
		
		public IdentityStore store() {
			return store;
		}
		
	}
	
	public static class Trigger {

		private final Path base;
		private final IdentityKey<Flow> name;
		private final Function<ConfigurerProvider, OperationConfigurer> supplier;
		
		public Trigger(Path base, IdentityKey<Flow> name, Function<ConfigurerProvider, OperationConfigurer> supplier) {
			this.base = base;
			this.name = name;
			this.supplier = supplier;
		}

		public Path base() {
			return base;
		}
		
		public IdentityKey<Flow> key() {
			return name;
		}
		
		public Function<ConfigurerProvider,OperationConfigurer> supplier() {
			return supplier;
		}
		
	}

	
	public static class InitFlow {
		
		public final Path name;
		public final Function<ConfigurerProvider, OperationConfigurer> supplier;
		public final IdentityStore store;
		public final Consumer<Flow> consumer;
		
		public InitFlow(Path name,
				Function<ConfigurerProvider, OperationConfigurer> supplier,
				IdentityStore store,
				Consumer<Flow> consumer) {
			this.name = name;
			this.supplier = supplier;
			this.store = store;
			this.consumer = consumer;
		}
		
	}

	public void status(Function<IdentityStore, StatusDataProvider> c) {
		includeDefaultStatus = false;
		collector.statuses.add(() -> StatusProvider.create(path.slashes(), info.version(), c.apply(store)));
	}
	
	public ModuleSetup initflow(String name, ConfigBody body, Consumer<InitFlowSetup> init) {
		Function<ConfigurerProvider, OperationConfigurer> supplier = provider -> configure(new SequenceConfigurer(provider), body);
		collector.initflows.add(new InitFlow(path.add(name), supplier, store, flow -> {
			init.accept(new InitFlowSetup(flow, store));
		}));
		return this;
	}
	
	public ModuleSetup trigger(String name, ConfigBody body, Consumer<SingleFlowRegistration> c) {
		return trigger(IdentityKey.named(name), body, c);
	}

	public ModuleSetup trigger(String name, Function<ConfigurerProvider, OperationConfigurer> supplier, Consumer<SingleFlowRegistration> c) {
		return trigger(IdentityKey.named(name), supplier, c);
	}
	
	public ModuleSetup triggers(Map<IdentityKey<Flow>,Function<ConfigurerProvider, OperationConfigurer>> suppliers, Consumer<MultiFlowRegistration> cs) {
		collector.triggers.add(new TriggerCollection(suppliers.entrySet().stream().map(e -> new Trigger(path, e.getKey(), e.getValue())).collect(toList()), cs, store));
		return this;
	}

	private ModuleSetup trigger(IdentityKey<Flow> key, ConfigBody body, Consumer<SingleFlowRegistration> c) {
		return trigger(key, provider -> configure(new SequenceConfigurer(provider), body), c);
	}
	
	private ModuleSetup trigger(IdentityKey<Flow> key, Function<ConfigurerProvider, OperationConfigurer> supplier, Consumer<SingleFlowRegistration> c) {
		Map<IdentityKey<Flow>,Function<ConfigurerProvider, OperationConfigurer>> suppliers = new HashMap<>();
		suppliers.put(key, supplier);
		return triggers(suppliers, m -> c.accept(m.singleFor(key)));
	}
	
	protected Optional<FlowSegment> buildFlowSegment() {
		if (segments.isEmpty()) return Optional.empty();
		List<FlowSegment> built = segments.stream().map(Supplier<FlowSegment>::get).collect(toList());
		return Optional.of(createLabelSegment(path.slashes(), seq(built)));
	}
	
}