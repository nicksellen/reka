package reka.core.runtime;

import java.util.concurrent.ExecutorService;

import reka.api.data.Data;
import reka.api.data.MutableData;
import reka.core.runtime.handlers.ActionHandler;
import reka.core.runtime.handlers.ErrorHandler;
import reka.core.runtime.handlers.stateful.NodeState;

public interface FlowContext {
	long flowId();
	long started();
    NodeState stateFor(int id);
    //Subscriber subscriber();
    ExecutorService executor();
    void execute(Runnable runnable);
	void call(ActionHandler next, ErrorHandler error, MutableData data);
	
    void error(Data data, Throwable t);
    void halted();
	void end(MutableData data);
}
