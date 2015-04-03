package reka.module.setup;

import java.util.Collection;

import reka.api.Path;
import reka.flow.FlowSegment;
import reka.flow.builder.FlowSegments;

class SequentialCollector extends AbstractOperationCollector {

	public SequentialCollector(Path basename, ModuleSetupContext ctx) {
		super(basename, ctx);
	}

	@Override
	FlowSegment build(Collection<FlowSegment> segments) {
		return FlowSegments.seq(segments);
	}
	
}