package reka.core.runtime;

import static reka.util.Util.unwrap;

import java.util.concurrent.ExecutorService;

import reka.api.data.Data;
import reka.api.data.MutableData;
import reka.core.runtime.handlers.ActionHandler;
import reka.core.runtime.handlers.ErrorHandler;
import reka.core.runtime.handlers.HaltedHandler;
import reka.core.runtime.handlers.stateful.NodeState;

public interface FlowContext {
	
	public static final HaltedHandler DEFAULT_HALTED_HANDLER = new HaltedHandler() {

		@Override
		public void halted(FlowContext context) {
			context.halted();
		}
		
	};
	
	public static final ErrorHandler DEFAULT_ERROR_HANDLER = new ErrorHandler(){

		@Override
		public void error(Data data, FlowContext context, Throwable t) {
			context.error(data, unwrap(t));
		}
		
	};
	
    ExecutorService operationExecutor();
    ExecutorService coordinationExecutor();
    
	long flowId();
    long contextId();
	long started();
	boolean statsEnabled();
    
    // call from any thread
	void handleAction(ActionHandler next, ErrorHandler error, MutableData data);
	void handleHalted(HaltedHandler halted);
	void handleError(ErrorHandler error, Data data, Throwable t);
	
	// only call these from the coordinator thread
	// TODO: make them more hidden...
    NodeState stateFor(int id);
    void error(Data data, Throwable t);
    void halted();
	void end(MutableData data);
	
}
