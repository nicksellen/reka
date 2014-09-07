package reka.core.bundle;

import static java.util.stream.Collectors.toList;
import static reka.config.configurer.Configurer.configure;
import static reka.core.builder.FlowSegments.label;
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

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

import reka.api.IdentityKey;
import reka.api.IdentityStore;
import reka.api.Path;
import reka.api.data.Data;
import reka.api.data.MutableData;
import reka.api.flow.Flow;
import reka.api.flow.FlowSegment;
import reka.api.run.AsyncOperation;
import reka.api.run.SyncOperation;
import reka.config.Config;
import reka.config.ConfigBody;
import reka.core.config.ConfigurerProvider;
import reka.core.config.SequenceConfigurer;
import reka.nashorn.OperationsConfigurer;

public class ModuleSetup {
	
	private final Path path;
	private final IdentityStore store;
	private final List<Supplier<FlowSegment>> segments = new ArrayList<>();
	private final List<Consumer<IdentityStore>> shutdownHandlers = new ArrayList<>();
	private final List<TriggerCollection> triggers = new ArrayList<>();
	private final Map<Path,FlowSegmentBiFunction> operations = new HashMap<>();
	
	public ModuleSetup(Path path, IdentityStore store) {
		this.path = path;
		this.store = store;
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
	
	public ModuleSetup setupInitializer(Consumer<ModuleOperationSetup> seq) {
		OperationSetup e = new SequentialCollector(store);
		seq.accept(new ModuleOperationSetup(e));
		segments.add(e);
		return this;
	}
	
	/*
	 * wraps the fullon OperationSetup and only allows direct operations to be defined, which just see the store
	 */
	public static class ModuleOperationSetup {
		
		private final OperationSetup ops;
		
		public ModuleOperationSetup(OperationSetup ops) {
			this.ops = ops;
		}
		
		public ModuleOperationSetup run(String name, Consumer<IdentityStore> c) {
			ops.add(name, store -> {
				return new SyncOperation() {
					
					@Override
					public MutableData call(MutableData data) {
						c.accept(store);
						return data;
					}
				};
			});
			return this;
		}
		public ModuleOperationSetup runAsync(String name, BiConsumer<IdentityStore, DoneCallback> c) {
			ops.add(name, store -> {
				return new AsyncOperation() {
					
					@Override
					public ListenableFuture<MutableData> call(MutableData data) {
						SettableFuture<MutableData> future = SettableFuture.create();
						c.accept(store, () -> future.set(data));
						return future;
					}
				};
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
		shutdownHandlers.add(handler);
	}
	
	public ModuleSetup operation(Path name, Function<ConfigurerProvider,OperationsConfigurer> c) {
		operations.put(path.add(name), (provider, config) -> {
			return configure(c.apply(provider), config).bind(store);
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
		private final Function<ConfigurerProvider, OperationsConfigurer> supplier;
		
		public Trigger(Path base, IdentityKey<Flow> name, Function<ConfigurerProvider, OperationsConfigurer> supplier) {
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
		
		public Function<ConfigurerProvider,OperationsConfigurer> supplier() {
			return supplier;
		}
		
	}
	
	public ModuleSetup trigger(String name, ConfigBody body, Consumer<SingleFlowRegistration> c) {
		return trigger(IdentityKey.named(name), body, c);
	}

	public ModuleSetup trigger(String name, Function<ConfigurerProvider, OperationsConfigurer> supplier, Consumer<SingleFlowRegistration> c) {
		return trigger(IdentityKey.named(name), supplier, c);
	}
	
	public ModuleSetup triggers(Map<IdentityKey<Flow>,Function<ConfigurerProvider, OperationsConfigurer>> suppliers, Consumer<MultiFlowRegistration> cs) {
		triggers.add(new TriggerCollection(suppliers.entrySet().stream().map(e -> new Trigger(path, e.getKey(), e.getValue())).collect(toList()), cs, store));
		return this;
	}

	private ModuleSetup trigger(IdentityKey<Flow> key, ConfigBody body, Consumer<SingleFlowRegistration> c) {
		return trigger(key, provider -> configure(new SequenceConfigurer(provider), body), c);
	}
	
	private ModuleSetup trigger(IdentityKey<Flow> key, Function<ConfigurerProvider, OperationsConfigurer> supplier, Consumer<SingleFlowRegistration> c) {
		Map<IdentityKey<Flow>,Function<ConfigurerProvider, OperationsConfigurer>> suppliers = new HashMap<>();
		suppliers.put(key, supplier);
		return triggers(suppliers, m -> c.accept(m.singleFor(key)));
	}
	
	public Optional<FlowSegment> buildFlowSegment() {
		if (segments.isEmpty()) return Optional.empty();
		List<FlowSegment> built = segments.stream().map(Supplier<FlowSegment>::get).collect(toList());
		return Optional.of(label(path.slashes(), seq(built)));
	}
	
	public Map<Path,FlowSegmentBiFunction> providers() {
		return operations;
	}
	
	public List<TriggerCollection> triggers() {
		return triggers;
	}
	
	public List<Consumer<IdentityStore>> shutdownHandlers() {
		return shutdownHandlers;
	}
	
}