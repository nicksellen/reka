package reka.core.setup;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

import reka.api.Path;
import reka.api.data.MutableData;
import reka.api.flow.FlowSegment;
import reka.api.flow.SimpleFlowOperation;
import reka.api.run.RouteKey;
import reka.api.run.RouterOperation;

public interface OperationSetup extends Supplier<FlowSegment> {
	
	public static OperationSetup createSequentialCollector(Path basename, ModuleSetupContext ctx) {
		return new SequentialCollector(basename, ctx);
	}
	
	public static interface RouterSetup {
		RouterSetup add(RouteKey key, OperationConfigurer configurer);
		RouterSetup addSequence(RouteKey key, Consumer<OperationSetup> seq);
		RouterSetup parallel(Consumer<OperationSetup> par);
	}
	
	MutableData meta();
	
	ModuleSetupContext ctx();
	
	OperationSetup label(String label);
	OperationSetup useNewContext();
	
	OperationSetup add(String name, Supplier<? extends SimpleFlowOperation> store);
	OperationSetup add(Supplier<FlowSegment> supplier);
	
	OperationSetup router(String name, Supplier<? extends RouterOperation> store, Consumer<RouterSetup> routes);
	
	OperationSetup add(OperationConfigurer configurer);
	
	OperationSetup sequential(Consumer<OperationSetup> seq);
	OperationSetup sequential(String label, Consumer<OperationSetup> seq);
	
	OperationSetup namedInputSeq(RouteKey key, Consumer<OperationSetup> seq);
	OperationSetup namedInput(RouteKey key, OperationConfigurer configurer);
	
	OperationSetup parallel(Consumer<OperationSetup> par);
	OperationSetup parallel(String label, Consumer<OperationSetup> par);
	
	<T> OperationSetup eachParallel(Iterable<T> it, BiConsumer<T, OperationSetup> seq);
	
}