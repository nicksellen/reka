package reka.core.builder;

import java.util.function.Supplier;

import reka.api.flow.FlowNode;
import reka.api.flow.FlowOperation;
import reka.api.flow.SimpleFlowOperation;
import reka.api.run.RouterOperation;

public class OperationFlowNode extends AbstractFlowNode implements FlowNode {

	public static OperationFlowNode createNoOp() {
		OperationFlowNode node = new OperationFlowNode();
		node.isNoOp(true);
		return node;
	}
	
	public static OperationFlowNode createNode(String name, Supplier<? extends SimpleFlowOperation> supplier) {
		OperationFlowNode node = new OperationFlowNode();
		node.name(name).supplier(new OperationSupplier<FlowOperation>() {

			@Override
			public FlowOperation get() {
				return supplier.get();
			}

			@Override
			public boolean isRouter() {
				return false;
			}
			
		});
		return node;
	}
	
	public static OperationFlowNode createRouterNode(String name, Supplier<? extends RouterOperation> supplier) {
		OperationFlowNode node = new OperationFlowNode();
		node.name(name).supplier(new OperationSupplier<FlowOperation>() {

			@Override
			public FlowOperation get() {
				return supplier.get();
			}

			@Override
			public boolean isRouter() {
				return true;
			}
		});
		return node;
	}
	
	private OperationFlowNode() {
	}
	
}