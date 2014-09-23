package reka.core.setup;

import java.util.function.Supplier;

import reka.api.IdentityStore;
import reka.api.Path;
import reka.api.flow.FlowSegment;

public interface OperationConfigurer {
	
	void setup(OperationSetup ops);
	
	default Supplier<FlowSegment> bind(Path basename, IdentityStore store) {
		OperationSetup collector = OperationSetup.createSequentialCollector(basename, store);
		setup(collector);
		return collector;
	}
	
}
