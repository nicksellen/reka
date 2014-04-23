package reka.api.flow;

import reka.api.run.OperationSupplier;

public interface FlowNode extends FlowSegment {

	boolean hasOperationSupplier();
    OperationSupplier<?> operationSupplier();
    
	boolean hasEmbeddedFlow();
	FlowDependency embeddedFlowNode();
	
	boolean isSubscribeable();
	
	boolean isStart();
	boolean isEnd();
	boolean shouldUseAnotherThread();
	
}
