package reka.core.builder;

import java.util.function.Supplier;

import reka.api.flow.FlowNode;
import reka.api.flow.FlowOperation;
import reka.api.flow.SimpleFlowOperation;
import reka.api.run.AsyncOperation;
import reka.api.run.Operation;
import reka.api.run.RouterOperation;

public class OperationFlowNode extends AbstractFlowNode implements FlowNode {

	private OperationFlowNode() {
	}
	
	public static OperationFlowNode noop() {
		OperationFlowNode node = new OperationFlowNode();
		node.isNoOp(true);
		return node;
	}
	
	public static OperationFlowNode operation(String name, Operation operation) {
		return node(name, () -> operation);
	}
	
	public static OperationFlowNode asyncOperation(String name, AsyncOperation operation) {
		return node(name, () -> operation);
	}
	
	public static OperationFlowNode router(String name, RouterOperation router) {
		return routerNode(name, () -> router);
	}
	
	public static OperationFlowNode node(String name, Supplier<? extends SimpleFlowOperation> supplier) {
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
	
	public static OperationFlowNode routerNode(String name, Supplier<? extends RouterOperation> supplier) {
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
	
}