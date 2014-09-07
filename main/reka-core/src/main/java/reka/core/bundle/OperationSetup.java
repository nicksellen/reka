package reka.core.bundle;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import reka.api.IdentityStore;
import reka.api.data.MutableData;
import reka.api.flow.FlowSegment;
import reka.api.flow.SimpleFlowOperation;
import reka.api.run.RoutingOperation;
import reka.core.bundle.ModuleSetup.DoneCallback;
import reka.nashorn.OperationsConfigurer;

public interface OperationSetup extends Supplier<FlowSegment> {
	
	public static OperationSetup createSequentialCollector(IdentityStore store) {
		return new SequentialCollector(store);
	}
	
	MutableData meta();
	
	OperationSetup label(String label);
	
	OperationSetup add(String name, Function<IdentityStore,? extends SimpleFlowOperation> c);
	OperationSetup addRouter(String name, Function<IdentityStore,? extends RoutingOperation> c);
	
	OperationSetup add(Supplier<FlowSegment> supplier);
	
	OperationSetup add(OperationsConfigurer configurer);
	
	OperationSetup sequential(Consumer<OperationSetup> seq);
	OperationSetup sequential(String label, Consumer<OperationSetup> seq);

	OperationSetup routeSeq(String name, Consumer<OperationSetup> seq);
	OperationSetup route(String name, OperationsConfigurer configurer);
	
	OperationSetup parallel(Consumer<OperationSetup> par);
	OperationSetup parallel(String label, Consumer<OperationSetup> par);
	
	<T> OperationSetup eachParallel(Iterable<T> it, BiConsumer<T, OperationSetup> seq);
	
}