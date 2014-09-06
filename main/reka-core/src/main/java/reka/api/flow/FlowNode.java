package reka.api.flow;

import java.util.function.Supplier;

public interface FlowNode extends FlowSegment {
	
	boolean hasOperationSupplier();
    Supplier<? extends FlowOperation> operationSupplier();
    
	boolean hasEmbeddedFlow();
	FlowDependency embeddedFlowNode();
	
	boolean isSubscribeable();
	
	boolean isStart();
	boolean isEnd();
	boolean shouldUseAnotherThread();
	
}
