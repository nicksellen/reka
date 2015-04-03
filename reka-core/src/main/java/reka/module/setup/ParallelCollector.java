package reka.module.setup;

import static reka.flow.builder.FlowSegments.createParallelSegment;

import java.util.Collection;

import reka.flow.FlowSegment;
import reka.util.Path;

class ParallelCollector extends AbstractOperationCollector {

	public ParallelCollector(Path basename, ModuleSetupContext ctx) {
		super(basename, ctx);
	}

	@Override
	FlowSegment build(Collection<FlowSegment> segments) {
		return createParallelSegment(segments);
	}
	
}