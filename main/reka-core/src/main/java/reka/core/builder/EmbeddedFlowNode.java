package reka.core.builder;

import reka.api.Path;

public class EmbeddedFlowNode extends AbstractFlowNode {
	
	public EmbeddedFlowNode(String name, String embeddedFlowName) {
		name(name).embeddedFlowNode(() -> Path.path(embeddedFlowName));
	}
	
}
