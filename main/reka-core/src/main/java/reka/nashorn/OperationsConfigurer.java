package reka.nashorn;

import java.util.function.Supplier;

import reka.api.IdentityStore;
import reka.api.flow.FlowSegment;
import reka.core.bundle.OperationSetup;

public interface OperationsConfigurer {
	
	void setup(OperationSetup ops);
	
	default Supplier<FlowSegment> bind(IdentityStore store) {
		OperationSetup collector = OperationSetup.createSequentialCollector(store);
		setup(collector);
		return collector;
	}
	
}
