package reka.flow.builder;

public class EndFlowNode extends AbstractFlowNode {

	public EndFlowNode(String name) {
		name(name).isEnd(true);
	}

}
