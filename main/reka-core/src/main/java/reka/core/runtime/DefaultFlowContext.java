package reka.core.runtime;

import static com.google.common.base.Preconditions.checkState;

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
	
	private final static ThreadLocal<Long> num = ThreadLocal.withInitial(() -> 0L);

	private final ExecutorService executor;
	private final Map<Integer,NodeState> states = new HashMap<>();
	private final EverythingSubscriber subscriber;
	private final long initialThreadId;
	private final long id;
	private final long flowId;
	private final long started;
	
	public DefaultFlowContext(long flowId, ExecutorService executor, EverythingSubscriber subscriber) {
		this.executor = executor;
		this.subscriber = subscriber;
		this.initialThreadId = Thread.currentThread().getId();
		long offset = num.get(); 
		this.id = offset;
		this.flowId = flowId;
		num.set(offset + 1);
		started = System.nanoTime();
	}

	@Override
    public NodeState stateFor(int id) {
		NodeState state = states.get(id);
		if (state == null) {
			state = new DefaultNodeState();
			states.put(id, state);
		}
		return state;
	}
	
	@Override
	public long id() {
		return id;
	}
	
	@Override
	public long flowId() {
		return flowId;
	}
	
	@Override
	public long initialThreadId() {
		return initialThreadId;
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
	
	@SuppressWarnings("unused")
	private void checkThreadId() {
		try {
			checkState(Thread.currentThread().getId() == initialThreadId, "thread id changed!");
		} catch (Throwable t) {
			t.printStackTrace();
			throw t;
		}
	}

	@Override
	public void execute(Runnable runnable) {
		executor.execute(runnable);
	}

	@Override
	public void call(ActionHandler next, ErrorHandler error, MutableData data) {
		if (Thread.currentThread().getId() == initialThreadId) {
			next.call(data, this);
		} else {
			execute(() -> {
				try {
					next.call(data, this);
				} catch (Throwable t) {
					error.error(data, this, t);
				}
			});
		}	
	}
}