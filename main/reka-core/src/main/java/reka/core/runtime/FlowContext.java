package reka.core.runtime;

import java.util.concurrent.ExecutorService;

import reka.api.data.Data;
import reka.api.data.MutableData;
import reka.core.runtime.handlers.ActionHandler;
import reka.core.runtime.handlers.ErrorHandler;
import reka.core.runtime.handlers.stateful.NodeState;

public interface FlowContext {
	
    ExecutorService operationExecutor();
    ExecutorService coordinationExecutor();
    
	long flowId();
	long started();
	boolean statsEnabled();
    
    // call from any thread
	void call(ActionHandler next, ErrorHandler error, MutableData data);
	
	// only call these from the coordinator thread
    NodeState stateFor(int id);
    void error(Data data, Throwable t);
    void halted();
	void end(MutableData data);
	
}
