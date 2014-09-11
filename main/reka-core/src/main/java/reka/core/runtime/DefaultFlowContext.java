package reka.core.runtime;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import reka.api.data.MutableData;
import reka.api.run.EverythingSubscriber;
import reka.core.runtime.handlers.ActionHandler;
import reka.core.runtime.handlers.ErrorHandler;
import reka.core.runtime.handlers.stateful.DefaultNodeState;
import reka.core.runtime.handlers.stateful.NodeState;

public class DefaultFlowContext implements FlowContext {
	
	public static FlowContext get(long flowId, ExecutorService executor, EverythingSubscriber subscriber) {
		return new DefaultFlowContext(flowId, executor, subscriber);
	}

	private final ExecutorService executor;
	private final Map<Integer,NodeState> states = new HashMap<>();
	private final EverythingSubscriber subscriber;
	private final long flowId;
	private final long started;
	
	private DefaultFlowContext(long flowId, ExecutorService executor, EverythingSubscriber subscriber) {
		this.executor = executor;
		this.subscriber = subscriber;
		this.flowId = flowId;
		started = System.nanoTime();
	}

	@Override
    public NodeState stateFor(int id) {
		NodeState state = states.get(id);
		if (state == null) {
			state = DefaultNodeState.get();
			states.put(id, state);
		}
		return state;
	}
	
	@Override
	public long flowId() {
		return flowId;
	}
	
	@Override
	public long started() {
		return started;
	}
	
	@Override
    public EverythingSubscriber subscriber() {
		return subscriber;
	}
	
	@Override
    public ExecutorService executor() {
		return executor;
	}

	@Override
	public void execute(Runnable runnable) {
		executor.execute(runnable);
	}

	@Override
	public void call(ActionHandler next, ErrorHandler error, MutableData data) {
		execute(() -> {
			try {
				next.call(data, this);
			} catch (Throwable t) {
				error.error(data, this, t);
			}
		});
	}

	@Override
	public void end() {
		// no-op
	}
}