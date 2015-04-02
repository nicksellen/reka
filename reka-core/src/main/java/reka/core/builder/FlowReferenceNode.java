package reka.core.builder;

import reka.api.Path;

public class FlowReferenceNode extends AbstractFlowNode {
	
	public FlowReferenceNode(String name, Path flowName) {
		name(name).flowReferenceNode(() -> flowName);
	}
	
}
