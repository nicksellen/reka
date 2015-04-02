package reka.api.flow;

import reka.core.builder.OperationSupplier;

public interface FlowNode extends FlowSegment {
	
	boolean hasOperationSupplier();
    OperationSupplier<?> operationSupplier();
    
	boolean hasFlowReference();
	FlowReference flowReferenceNode();
	
	boolean isNoOp();
	
	boolean isStart();
	boolean isEnd();
	
	default boolean isRouter() {
		return hasOperationSupplier() && operationSupplier().isRouter();
	}
	
}
