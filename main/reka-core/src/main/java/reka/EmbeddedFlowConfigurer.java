package reka;

import reka.config.configurer.annotations.Conf;
import reka.core.builder.EmbeddedFlowNode;
import reka.core.bundle.OperationSetup;
import reka.nashorn.OperationsConfigurer;

public class EmbeddedFlowConfigurer implements OperationsConfigurer {

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
