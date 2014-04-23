package reka.workflow;

import reka.core.bundle.UseConfigurer;
import reka.core.bundle.UseInit;

public class UseWorkflow extends UseConfigurer {

	@Override
	public void setup(UseInit init) {
		init.operation("save", () -> new WorkflowSaveConfigurer());
		init.operation("load", () -> new WorkflowLoadConfigurer());
	}

}
