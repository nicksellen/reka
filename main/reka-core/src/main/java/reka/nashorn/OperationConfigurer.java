package reka.nashorn;

import java.util.function.Supplier;

import reka.api.IdentityStore;
import reka.api.flow.FlowSegment;
import reka.core.setup.OperationSetup;

public interface OperationConfigurer {
	
	void setup(OperationSetup ops);
	
	default Supplier<FlowSegment> bind(IdentityStore store) {
		OperationSetup collector = OperationSetup.createSequentialCollector(store);
		setup(collector);
		return collector;
	}
	
}
