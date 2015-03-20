package reka.core.setup;

import static reka.core.builder.FlowSegments.createParallelSegment;

import java.util.Collection;

import reka.api.Path;
import reka.api.flow.FlowSegment;

class ParallelCollector extends AbstractOperationCollector {

	public ParallelCollector(Path basename, ModuleSetupContext ctx) {
		super(basename, ctx);
	}

	@Override
	FlowSegment build(Collection<FlowSegment> segments) {
		return createParallelSegment(segments);
	}
	
}