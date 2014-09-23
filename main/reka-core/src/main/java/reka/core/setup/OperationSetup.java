package reka.core.setup;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import reka.api.IdentityStore;
import reka.api.Path;
import reka.api.data.MutableData;
import reka.api.flow.FlowSegment;
import reka.api.flow.SimpleFlowOperation;
import reka.api.run.RouteKey;
import reka.api.run.RouterOperation;

public interface OperationSetup extends Supplier<FlowSegment> {
	
	public static OperationSetup createSequentialCollector(Path basename, IdentityStore store) {
		return new SequentialCollector(basename, store);
	}
	
	public static interface RouterSetup {
		RouterSetup add(RouteKey key, OperationConfigurer configurer);
		RouterSetup addSequence(RouteKey key, Consumer<OperationSetup> seq);
		RouterSetup parallel(Consumer<OperationSetup> par);
	}
	
	MutableData meta();
	
	OperationSetup label(String label);
	
	OperationSetup add(String name, Function<IdentityStore,? extends SimpleFlowOperation> store);
	OperationSetup add(Supplier<FlowSegment> supplier);
	
	OperationSetup router(String name, Function<IdentityStore,? extends RouterOperation> store, Consumer<RouterSetup> routes);
	
	OperationSetup add(OperationConfigurer configurer);
	
	OperationSetup sequential(Consumer<OperationSetup> seq);
	OperationSetup sequential(String label, Consumer<OperationSetup> seq);
	
	OperationSetup namedInputSeq(RouteKey key, Consumer<OperationSetup> seq);
	OperationSetup namedInput(RouteKey key, OperationConfigurer configurer);
	
	OperationSetup parallel(Consumer<OperationSetup> par);
	OperationSetup parallel(String label, Consumer<OperationSetup> par);
	
	<T> OperationSetup eachParallel(Iterable<T> it, BiConsumer<T, OperationSetup> seq);

	OperationSetup defer(Supplier<FlowSegment> supplier);
	
}