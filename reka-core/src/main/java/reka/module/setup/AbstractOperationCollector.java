package reka.module.setup;

import static java.util.stream.Collectors.toList;
import static reka.flow.builder.FlowSegments.createLabelSegment;
import static reka.flow.builder.FlowSegments.createMetaSegment;
import static reka.flow.builder.FlowSegments.createNamedInputSegment;
import static reka.flow.builder.OperationFlowNode.node;
import static reka.flow.builder.OperationFlowNode.routerNode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

import reka.data.MutableData;
import reka.data.memory.MutableMemoryData;
import reka.flow.FlowSegment;
import reka.flow.SimpleFlowOperation;
import reka.flow.ops.RouteKey;
import reka.flow.ops.RouterOperation;
import reka.util.Path;

abstract class AbstractOperationCollector implements OperationSetup {
	
	private final Path basename;
	
	private final ModuleSetupContext ctx;
	private final MutableData meta = MutableMemoryData.create();
	private final List<Supplier<FlowSegment>> suppliers = new ArrayList<>();
	
	private String label;
	private boolean newContext = false;
	
	public AbstractOperationCollector(Path basename, ModuleSetupContext ctx) {
		this.basename = basename;
		this.ctx = ctx;
	}
	
	@Override
	public ModuleSetupContext ctx() {
		return ctx;
	}
	
	@Override
	public final OperationSetup label(String label) {
		this.label = label;
		return this;
	}

	@Override
	public final OperationSetup useNewContext() {
		this.newContext = true;
		return this;
	}
	
	@Override
	public final MutableData meta() {
		return meta;
	}

	@Override
	public final OperationSetup add(String name, Supplier<? extends SimpleFlowOperation> c) {
		addNode(name, c);
		return this;
	}

	@Override
	public final OperationSetup router(String name, 
			                           Supplier<? extends RouterOperation> router, 
			                           Consumer<RouterSetup> routes) {
		
		suppliers.add(() -> routerNode(basename.add(name).slashes(), () -> router.get()));
		
		parallel(par -> {
			routes.accept(new RouterSetup(){

				@Override
				public RouterSetup add(RouteKey key, OperationConfigurer configurer) {
					par.namedInput(key, configurer);
					return this;
				}

				@Override
				public RouterSetup addSequence(RouteKey key, Consumer<OperationSetup> seq) {
					par.namedInputSeq(key, seq);
					return this;
				}

				@Override
				public RouterSetup parallel(Consumer<OperationSetup> consumer) {
					par.parallel(consumer);
					return this;
				}
				
			});
		});
		
		return this;
	}
	
	@Override
	public final OperationSetup add(Supplier<FlowSegment> supplier) {
		suppliers.add(supplier);
		return this;
	}
	
	@Override
	public final OperationSetup add(OperationConfigurer configurer) {
		add(() -> {
			OperationSetup ops = OperationSetup.createSequentialCollector(basename, ctx);
			configurer.setup(ops);	
			return ops.get();
		});
		return this;
	}
	
	@Override
	public final <T> OperationSetup eachParallel(Iterable<T> it, BiConsumer<T, OperationSetup> c) {
		OperationSetup e = new ParallelCollector(basename, ctx);
		for (T v : it) {
			e.sequential(s -> c.accept(v, s));
		}
		add(e);
		return this;
	}

	@Override
	public final OperationSetup sequential(Consumer<OperationSetup> seq) {
		OperationSetup e = new SequentialCollector(basename, ctx);
		seq.accept(e);
		add(e);
		return this;
	}

	@Override
	public final OperationSetup sequential(String label, Consumer<OperationSetup> seq) {
		OperationSetup e = new SequentialCollector(basename, ctx);
		seq.accept(e);
		add(() -> createLabelSegment(label, e.get()));
		return this;
	}

	@Override
	public final OperationSetup namedInputSeq(RouteKey key, Consumer<OperationSetup> seq) {
		OperationSetup e = new SequentialCollector(basename, ctx);
		seq.accept(e);
		add(() -> createNamedInputSegment(key, e.get()));
		return this;
	}

	@Override
	public final OperationSetup namedInput(RouteKey key, OperationConfigurer configurer) {
		add(() -> {
			OperationSetup ops = OperationSetup.createSequentialCollector(basename, ctx);
			configurer.setup(ops);	
			return createNamedInputSegment(key, ops.get());
		});
		return this;
	}

	@Override
	public final OperationSetup parallel(Consumer<OperationSetup> par) {
		OperationSetup e = new ParallelCollector(basename, ctx);
		par.accept(e);
		add(e);
		return this;
	}
	
	@Override
	public final OperationSetup parallel(String label, Consumer<OperationSetup> par) {
		OperationSetup e = new ParallelCollector(basename, ctx);
		par.accept(e);
		add(() -> createLabelSegment(label, e.get()));
		return this;
	}

	@Override
	public final FlowSegment get() {
		List<FlowSegment> built = suppliers.stream().map(Supplier<FlowSegment>::get).collect(toList());
		FlowSegment segment = build(built);
		if (meta.size() > 0) segment = createMetaSegment(segment, meta);
		if (label != null && !label.isEmpty()) segment = createLabelSegment(label, segment);
		if (newContext) segment = segment.withNewContext();
		return segment;
	}

	private void addNode(String name, Supplier<? extends SimpleFlowOperation> c) {
		add(() -> node(basename.add(name).slashes(), () -> c.get()));
	}
	
	abstract FlowSegment build(Collection<FlowSegment> segments);
	
}