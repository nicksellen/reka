package reka.extra;


import reka.command.CommandBundle;
import reka.core.bundle.RekaBundle;
import reka.elasticsearch.ElasticsearchBundle;
import reka.javascript.JavascriptBundle;
import reka.validation.ValidatorBundle;
import reka.workflow.WorkflowBundle;

public class ExtraBundle implements RekaBundle {
	
	@Override
	public void setup(Setup setup) {
		setup.bundles(
			new ValidatorBundle(),
			new JavascriptBundle(),
			new ElasticsearchBundle(),
			new WorkflowBundle(), 
			new CommandBundle());
	}
}
