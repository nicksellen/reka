package reka.core.setup;

import java.util.Collection;

import reka.api.IdentityStore;
import reka.api.flow.FlowSegment;
import reka.core.builder.FlowSegments;

class SequentialCollector extends AbstractOperationCollector {

	public SequentialCollector(IdentityStore store) {
		super(store);
	}

	@Override
	FlowSegment build(Collection<FlowSegment> segments) {
		return FlowSegments.seq(segments);
	}
	
}