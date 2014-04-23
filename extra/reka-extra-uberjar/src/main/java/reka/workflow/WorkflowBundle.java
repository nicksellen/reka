package reka.workflow;

import static reka.api.Path.slashes;
import reka.core.bundle.RekaBundle;

public class WorkflowBundle implements RekaBundle {

	@Override
	public void setup(Setup setup) {
		setup.converter(new WorkflowConfigConverter());
		setup.use(slashes("workflow"), () -> new UseWorkflow());
	}

}
