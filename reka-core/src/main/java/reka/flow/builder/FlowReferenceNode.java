package reka.flow.builder;

import reka.api.Path;

public class FlowReferenceNode extends AbstractFlowNode {
	
	public FlowReferenceNode(String name, Path flowName) {
		name(name).flowReferenceNode(() -> flowName);
	}
	
}
