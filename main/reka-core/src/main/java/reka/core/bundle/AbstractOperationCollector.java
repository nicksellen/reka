package reka.core.bundle;

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import reka.api.IdentityStore;
import reka.api.data.MutableData;
import reka.api.flow.FlowSegment;
import reka.api.flow.SimpleFlowOperation;
import reka.api.run.RoutingOperation;
import reka.core.builder.FlowSegments;
import reka.core.builder.OperationFlowNode;
import reka.core.data.memory.MutableMemoryData;
import reka.nashorn.OperationsConfigurer;

abstract class AbstractOperationCollector implements OperationSetup {
	
	private final IdentityStore store;
	private final MutableData meta = MutableMemoryData.create();
	private final List<Supplier<FlowSegment>> segments = new ArrayList<>();
	
	private String label;
	
	public AbstractOperationCollector(IdentityStore store) {
		this.store = store;
	}
	
	@Override
	public final OperationSetup label(String label) {
		this.label = label;
		return this;
	}
	
	@Override
	public final MutableData meta() {
		return meta;
	}

	@Override
	public final OperationSetup add(String name, Function<IdentityStore,? extends SimpleFlowOperation> c) {
		addNode(name, c);
		return this;
	}

	@Override
	public final OperationSetup addRouter(String name, Function<IdentityStore,? extends RoutingOperation> c) {
		segments.add(() -> {
			return OperationFlowNode.router(name, () -> {
				return c.apply(store);
			});
		});
		return this;
	}
	
	@Override
	public final OperationSetup add(Supplier<FlowSegment> supplier) {
		segments.add(supplier);
		return this;
	}
	
	@Override
	public final OperationSetup add(OperationsConfigurer configurer) {
		segments.add(() -> {
			OperationSetup ops = OperationSetup.createSequentialCollector(store);
			configurer.setup(ops);	
			return ops.get();
		});
		return this;
	}
	
	@Override
	public final <T> OperationSetup eachParallel(Iterable<T> it, BiConsumer<T, OperationSetup> c) {
		OperationSetup e = new ParallelCollector(store);
		for (T v : it) {
			e.sequential(s -> {
				c.accept(v, s);
			});
		}
		segments.add(e);
		return this;
	}

	@Override
	public final OperationSetup sequential(Consumer<OperationSetup> seq) {
		OperationSetup e = new SequentialCollector(store);
		seq.accept(e);
		segments.add(e);
		return this;
	}

	@Override
	public final OperationSetup sequential(String label, Consumer<OperationSetup> seq) {
		OperationSetup e = new SequentialCollector(store);
		seq.accept(e);
		segments.add(() -> FlowSegments.label(label, e.get()));
		return this;
	}

	@Override
	public final OperationSetup routeSeq(String name, Consumer<OperationSetup> seq) {
		OperationSetup e = new SequentialCollector(store);
		seq.accept(e);
		segments.add(() -> FlowSegments.namedInput(name, e.get()));
		return this;
	}

	@Override
	public final OperationSetup route(String name, OperationsConfigurer configurer) {
		segments.add(() -> {
			OperationSetup ops = OperationSetup.createSequentialCollector(store);
			configurer.setup(ops);	
			return FlowSegments.namedInput(name, ops.get());
		});
		return this;
	}

	@Override
	public final OperationSetup parallel(Consumer<OperationSetup> par) {
		OperationSetup e = new ParallelCollector(store);
		par.accept(e);
		segments.add(e);
		return this;
	}
	

	@Override
	public final OperationSetup parallel(String label, Consumer<OperationSetup> par) {
		OperationSetup e = new ParallelCollector(store);
		par.accept(e);
		segments.add(() -> FlowSegments.label(label, e.get()));
		return this;
	}

	@Override
	public final FlowSegment get() {
		List<FlowSegment> built = segments.stream().map(Supplier<FlowSegment>::get).collect(toList());
		FlowSegment segment = build(built);
		if (meta.size() > 0) segment = FlowSegments.meta(segment, meta);
		if (label != null && !label.isEmpty()) segment = FlowSegments.label(label, segment);
		return segment;
	}
	
	private void addNode(String name, Function<IdentityStore,? extends SimpleFlowOperation> c) {
		segments.add(() -> {
			return OperationFlowNode.simple(name, () -> {
				return c.apply(store);
			});
		});
	}
	
	abstract FlowSegment build(Collection<FlowSegment> segments);
	
}