package reka.core.bundle;

import static java.util.stream.Collectors.toList;
import static reka.api.Path.slashes;
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

public class UseInit {
	
	public static interface UseExecutor extends Supplier<FlowSegment> {
		UseExecutor run(String name, SyncOperation operation);		
		UseExecutor runAsync(String name, AsyncOperation operation);
		UseExecutor sequential(Consumer<UseExecutor> seq);
		UseExecutor sequential(String label, Consumer<UseExecutor> seq);
		UseExecutor parallel(Consumer<UseExecutor> par);
		UseExecutor parallel(String label, Consumer<UseExecutor> par);
	}
	
	private abstract static class AbstractExecutor implements UseExecutor {
		
		private final List<Supplier<FlowSegment>> segments = new ArrayList<>();

		@Override
		public UseExecutor run(String name, SyncOperation operation) {
			segments.add(() -> sync(name, () -> operation));
			return this;
		}

		@Override
		public UseExecutor runAsync(String name, AsyncOperation operation) {
			segments.add(() -> async(name, () -> operation));
			return this;
		}

		@Override
		public UseExecutor sequential(Consumer<UseExecutor> seq) {
			UseExecutor e = new SequentialExecutor();
			seq.accept(e);
			segments.add(e);
			return this;
		}

		@Override
		public UseExecutor sequential(String label, Consumer<UseExecutor> seq) {
			UseExecutor e = new SequentialExecutor();
			seq.accept(e);
			segments.add(() -> FlowSegments.label(label, e.get()));
			return this;
		}

		@Override
		public UseExecutor parallel(Consumer<UseExecutor> par) {
			UseExecutor e = new ParallelExecutor();
			par.accept(e);
			segments.add(e);
			return this;
		}
		

		@Override
		public UseExecutor parallel(String label, Consumer<UseExecutor> par) {
			UseExecutor e = new ParallelExecutor();
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
	private final Map<Path,Supplier<TriggerConfigurer>> triggers = new HashMap<>();
	private final List<Trigger2> trigger2s = new ArrayList<>();
	private final Map<Path,Function<ConfigurerProvider,Supplier<FlowSegment>>> providers = new HashMap<>();
	
	public UseInit(Path path) {
		this.path = path;
	}
	
	public Path path() {
		return path;
	}
	
	public UseInit run(String name, SyncOperation operation) {
		segments.add(() -> sync(name, () -> operation));
		return this;
	}
	
	public UseInit parallel(Consumer<UseExecutor> parallel) {
		UseExecutor e = new ParallelExecutor();
		parallel.accept(e);
		segments.add(e);
		return this;
	}
	
	public UseInit runAsync(String name, AsyncOperation operation) {
		segments.add(() -> async(name, () -> operation));
		return this;
	}

	public void shutdown(String name, Runnable handler) {
		shutdownHandlers.add(handler);
	}
	
	public UseInit operation(String name, Supplier<Supplier<FlowSegment>> supplier) {
		providers.put(toPath(name), (provider) -> supplier.get());
		return this;
	}
	
	public UseInit operation(String name, Function<ConfigurerProvider,Supplier<FlowSegment>> provider) {
		providers.put(toPath(name), provider);
		return this;
	}

	public UseInit operation(Iterable<String> names, Supplier<Supplier<FlowSegment>> supplier) {
		Function<ConfigurerProvider,Supplier<FlowSegment>> provider = (p) -> supplier.get();
		for (String name : names) {
			operation(name, provider);
		}
		return this;
	}

	public UseInit operation(Iterable<String> names, Function<ConfigurerProvider,Supplier<FlowSegment>> provider) {
		for (String name : names) {
			operation(name, provider);
		}
		return this;
	}

	public UseInit trigger(Path triggerPath, Supplier<TriggerConfigurer> supplier) {
		triggers.put(path.add(triggerPath), supplier);
		return this;
	}
	
	public static class Registration2 {
		
		private final int applicationVersion;
		private final Flow flow;
		private final String identity;
		private final List<DeployedResource> resources = new ArrayList<>();
		private final List<PortAndProtocol> network = new ArrayList<>();
		 
		public Registration2(int applicationVersion, Flow flow, String identity) {
			this.applicationVersion = applicationVersion;
			this.flow = flow;
			this.identity = identity;
		}
		
		public int applicationVersion() {
			return applicationVersion;
		}
		
		public Flow flow() {
			return flow;
		}
		
		public String identity() {
			return identity;
		}
		
		public Registration2 resource(DeployedResource r) {
			resources.add(r);
			return this;
		}

		public void undeploy(Runnable r) {
			resource(new SimpleDeployedResource() {
				@Override
				public void undeploy(int version) {
					r.run();
				}
			});
		}
		
		public Registration2 network(int port, String protocol, Data details) {
			network.add(new PortAndProtocol(port, protocol, details));
			return this;
		}
		
		public List<DeployedResource> resources() {
			return resources;
		}
		
		public List<PortAndProtocol> network() {
			return network;
		}
		
	}
	
	public static class Trigger2 {
		
		private final Path name;
		private final Function<ConfigurerProvider, Supplier<FlowSegment>> supplier;
		private final Consumer<Registration2> consumer;
		
		public Trigger2(Path name,
				Function<ConfigurerProvider, Supplier<FlowSegment>> supplier,
				Consumer<Registration2> consumer) {
			this.name = name;
			this.supplier = supplier;
			this.consumer = consumer;
		}
		
		public Path name() {
			return name;
		}
		
		public Function<ConfigurerProvider, Supplier<FlowSegment>> supplier() {
			return supplier;
		}
		
		public Consumer<Registration2> consumer() {
			return consumer;
		}
		
	}
	
	public UseInit trigger2(String name, ConfigBody body, Consumer<Registration2> c) {
		return trigger2(name,  (provider) -> configure(new SequenceConfigurer(provider), body), c);
	}
	
	public UseInit trigger2(String name, Function<ConfigurerProvider, Supplier<FlowSegment>> supplier, Consumer<Registration2> c) {
		trigger2s.add(new Trigger2(path.add(name), supplier, c));
		return this;
	}

	private Path toPath(String name) {
		return name == null || "".equals(name.trim()) ? path : path.add(slashes(name));
	}
	
	public Optional<FlowSegment> buildFlowSegment() {
		if (segments.isEmpty()) {
			return Optional.empty();
		} else {
			List<FlowSegment> built = segments.stream().map(Supplier<FlowSegment>::get).collect(toList());
			return Optional.of(label(path.slashes(), seq(built)));
		}
	}
	
	public Map<Path,Function<ConfigurerProvider,Supplier<FlowSegment>>> providers() {
		return providers;
	}
	
	public Map<Path,Supplier<TriggerConfigurer>> triggers() {
		return triggers;
	}
	
	public List<Trigger2> trigger2s() {
		return trigger2s;
	}
	
	public List<Runnable> shutdownHandlers() {
		return shutdownHandlers;
	}
	
}