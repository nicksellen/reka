package reka;

import java.util.function.Supplier;

import reka.api.flow.FlowSegment;
import reka.configurer.annotations.Conf;
import reka.core.builder.EmbeddedFlowNode;

public class EmbeddedFlowConfigurer implements Supplier<FlowSegment> {

	private String name;
	
	@Conf.Val
	public EmbeddedFlowConfigurer name(String val) {
		name = val;
		return this;
	}
	
	@Override
	public FlowSegment get() {
		return new EmbeddedFlowNode(name, name);
	}

}
