package reka.core.runtime;

import static com.google.common.base.Preconditions.checkState;

import java.util.HashMap;
import java.util.Map;

import reka.api.run.EverythingSubscriber;
import reka.core.runtime.handlers.stateful.DefaultNodeState;
import reka.core.runtime.handlers.stateful.NodeState;

import com.google.common.util.concurrent.ListeningExecutorService;

public class DefaultFlowContext implements FlowContext {
	
	private final static ThreadLocal<Long> num = ThreadLocal.withInitial(() -> 0L);

	private final ListeningExecutorService executor;
	private final Map<Integer,NodeState> states = new HashMap<>();
	private final EverythingSubscriber subscriber;
	private final long threadId;
	private final long id;
	private final long flowId;
	private final long started;
	
	public DefaultFlowContext(long flowId, ListeningExecutorService executor, EverythingSubscriber subscriber) {
		this.executor = executor;
		this.subscriber = subscriber;
		this.threadId = Thread.currentThread().getId();
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
	public long threadId() {
		return threadId;
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
    public ListeningExecutorService executor() {
		return executor;
	}
	
	@SuppressWarnings("unused")
	private void checkThreadId() {
		try {
			checkState(Thread.currentThread().getId() == threadId, "thread id changed!");
		} catch (Throwable t) {
			t.printStackTrace();
			throw t;
		}
	}

	@Override
	public void execute(Runnable runnable) {
		executor.execute(runnable);
	}
}