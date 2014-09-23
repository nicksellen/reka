package reka;

import reka.config.configurer.annotations.Conf;
import reka.core.builder.EmbeddedFlowNode;
import reka.core.setup.OperationConfigurer;
import reka.core.setup.OperationSetup;

public class EmbeddedFlowConfigurer implements OperationConfigurer {

	private String name;
	
	@Conf.Val
	public EmbeddedFlowConfigurer name(String val) {
		name = val;
		return this;
	}
	
	@Override
	public void setup(OperationSetup ops) {
		ops.add(() -> new EmbeddedFlowNode(name, name));
	}

}
