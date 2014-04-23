package reka.core.runtime;

import reka.api.run.EverythingSubscriber;
import reka.core.runtime.handlers.stateful.NodeState;

import com.google.common.util.concurrent.ListeningExecutorService;

public interface FlowContext {
	long id();
	long flowId();
	long threadId();
	long started();
    NodeState stateFor(int id);
    EverythingSubscriber subscriber();
    ListeningExecutorService executor();
    void execute(Runnable runnable);
}
