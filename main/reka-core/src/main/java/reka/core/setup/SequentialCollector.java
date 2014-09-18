package reka.core.setup;

import java.util.Collection;

import reka.api.IdentityStore;
import reka.api.Path;
import reka.api.flow.FlowSegment;
import reka.core.builder.FlowSegments;

class SequentialCollector extends AbstractOperationCollector {

	public SequentialCollector(Path basename, IdentityStore store) {
		super(basename, store);
	}

	@Override
	FlowSegment build(Collection<FlowSegment> segments) {
		return FlowSegments.seq(segments);
	}
	
}