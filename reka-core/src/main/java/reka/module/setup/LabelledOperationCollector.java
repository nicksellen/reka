package reka.module.setup;

import java.util.Collection;

import reka.flow.FlowSegment;
import reka.flow.builder.FlowSegments;
import reka.util.Path;

public class LabelledOperationCollector extends SequentialCollector {

	private final String label;
	
	public LabelledOperationCollector(Path basename, ModuleSetupContext ctx, String label) {
		super(basename, ctx);
		this.label = label;
	}
	
	@Override
	FlowSegment build(Collection<FlowSegment> segments) {
		return FlowSegments.createLabelSegment(label, super.build(segments));
	}
	
}