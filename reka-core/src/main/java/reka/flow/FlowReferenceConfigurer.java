package reka.flow;

import static reka.api.Path.path;
import reka.config.configurer.annotations.Conf;
import reka.flow.builder.FlowReferenceNode;
import reka.module.setup.OperationConfigurer;
import reka.module.setup.OperationSetup;

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
