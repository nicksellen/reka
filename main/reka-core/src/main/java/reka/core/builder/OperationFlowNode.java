package reka.core.builder;

import reka.api.flow.FlowNode;
import reka.api.run.OperationSupplier;

public class OperationFlowNode extends AbstractFlowNode implements FlowNode {
	
	public static FlowNode node(String name, OperationSupplier<?> supplier) {
		return new OperationFlowNode(name, supplier);
	}
	
	protected OperationFlowNode(String name, OperationSupplier<?> supplier) {
		name(name).supplier(supplier);
	}
	
}