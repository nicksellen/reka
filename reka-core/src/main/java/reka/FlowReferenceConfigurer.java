package reka;

import static reka.api.Path.path;
import reka.config.configurer.annotations.Conf;
import reka.core.builder.FlowReferenceNode;
import reka.core.setup.OperationConfigurer;
import reka.core.setup.OperationSetup;

public class FlowReferenceConfigurer implements OperationConfigurer {

	private String name;
	
	@Conf.Val
	public FlowReferenceConfigurer name(String val) {
		name = val;
		return this;
	}
	
	@Override
	public void setup(OperationSetup ops) {
		ops.add(() -> new FlowReferenceNode(name, path(name)));
	}

}
