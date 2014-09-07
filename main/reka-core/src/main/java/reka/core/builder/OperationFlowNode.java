package reka.core.builder;

import java.util.function.Supplier;

import reka.api.flow.FlowNode;
import reka.api.flow.FlowOperation;
import reka.api.flow.SimpleFlowOperation;
import reka.api.run.RoutingOperation;

public class OperationFlowNode extends AbstractFlowNode implements FlowNode {
	
	/*
	public static FlowNode node(String name, OperationSupplier<?> supplier) {
		return new OperationFlowNode(name, supplier);
	}
	
	public OperationFlowNode(String name, OperationSupplier<?> supplier) {
		name(name).supplier(supplier);
	}
	*/
	
	public static OperationFlowNode simple(String name, Supplier<? extends SimpleFlowOperation> supplier) {
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
	
	public static OperationFlowNode router(String name, Supplier<? extends RoutingOperation> supplier) {
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