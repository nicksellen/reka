package reka.workflow;

import static reka.core.builder.FlowSegments.sync;

import java.util.function.Supplier;

import reka.api.flow.FlowSegment;

public class WorkflowSaveConfigurer implements Supplier<FlowSegment> {

	@Override
	public FlowSegment get() {
		return sync("workflow/save", () -> new WorkflowSaveOperation());
	}

}
