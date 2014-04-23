package reka.core.builder;

public class SubscribeableEndFlowNode extends AbstractFlowNode {

	public SubscribeableEndFlowNode(String name) {
		name(name).subscribeable(true).isEnd(true);
	}

}
