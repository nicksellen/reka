package reka.core.bundle;

import static java.util.stream.Collectors.toList;
import static reka.api.Path.slashes;
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

import reka.api.Path;
import reka.api.flow.FlowSegment;
import reka.api.run.AsyncOperation;
import reka.api.run.SyncOperation;
import reka.core.builder.FlowSegments;
import reka.core.config.ConfigurerProvider;

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
	
	public List<Runnable> shutdownHandlers() {
		return shutdownHandlers;
	}
	
}