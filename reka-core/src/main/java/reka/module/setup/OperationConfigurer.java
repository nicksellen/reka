package reka.module.setup;

import java.util.function.Supplier;

import reka.flow.FlowSegment;
import reka.util.Path;

public interface OperationConfigurer {
	
	void setup(OperationSetup ops);
	
	default Supplier<FlowSegment> bind(Path basename, ModuleSetupContext ctx) {
		OperationSetup collector = OperationSetup.createSequentialCollector(basename, ctx);
		setup(collector);
		return collector;
	}
	
}
