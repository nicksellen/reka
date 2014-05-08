package reka.extra;


import reka.command.CommandBundle;
import reka.core.bundle.RekaBundle;
import reka.elasticsearch.ElasticsearchBundle;
import reka.javascript.JavascriptBundle;
import reka.validation.ValidatorBundle;

public class ExtraBundle implements RekaBundle {
	
	@Override
	public void setup(Setup setup) {
		setup.bundles(
			new ValidatorBundle(),
			new JavascriptBundle(),
			new ElasticsearchBundle(),
			new CommandBundle());
	}
}
