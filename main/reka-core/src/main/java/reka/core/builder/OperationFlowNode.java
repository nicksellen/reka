package reka.core.builder;

import java.util.function.Supplier;

import reka.api.flow.FlowNode;
import reka.api.flow.FlowOperation;

public class OperationFlowNode extends AbstractFlowNode implements FlowNode {
	
	public static FlowNode node(String name, Supplier<? extends FlowOperation> supplier) {
		return new OperationFlowNode(name, supplier);
	}
	
	protected OperationFlowNode(String name, Supplier<? extends FlowOperation> supplier) {
		name(name).supplier(supplier);
	}
	
}