package reka.core.setup;

import java.util.Collection;

import reka.api.IdentityStore;
import reka.api.Path;
import reka.api.flow.FlowSegment;
import reka.core.builder.FlowSegments;

public class LabelledOperationCollector extends SequentialCollector {

	private final String label;
	
	public LabelledOperationCollector(Path basename, IdentityStore store, String label) {
		super(basename, store);
		this.label = label;
	}
	
	@Override
	FlowSegment build(Collection<FlowSegment> segments) {
		return FlowSegments.createLabelSegment(label, super.build(segments));
	}
	
}