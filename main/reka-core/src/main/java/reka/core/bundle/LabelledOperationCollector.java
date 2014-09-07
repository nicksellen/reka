package reka.core.bundle;

import java.util.Collection;

import reka.api.IdentityStore;
import reka.api.flow.FlowSegment;
import reka.core.builder.FlowSegments;

public class LabelledOperationCollector extends SequentialCollector {

	private final String label;
	
	public LabelledOperationCollector(IdentityStore store, String label) {
		super(store);
		this.label = label;
	}
	
	@Override
	FlowSegment build(Collection<FlowSegment> segments) {
		return FlowSegments.label(label, super.build(segments));
	}
	
}