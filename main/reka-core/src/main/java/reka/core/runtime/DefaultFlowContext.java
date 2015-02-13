package reka.core.runtime;

import static reka.util.Util.unchecked;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;

import reka.api.data.Data;
import reka.api.data.MutableData;
import reka.api.flow.Flow.FlowStats;
import reka.api.run.Subscriber;
import reka.core.runtime.handlers.ActionHandler;
import reka.core.runtime.handlers.ErrorHandler;
import reka.core.runtime.handlers.HaltedHandler;
import reka.core.runtime.handlers.stateful.DefaultNodeState;
import reka.core.runtime.handlers.stateful.NodeState;

public class DefaultFlowContext implements FlowContext {

	private static final AtomicLong contextIds = new AtomicLong();
	
	private final long contextId = contextIds.incrementAndGet();
	
	public static FlowContext create(long flowId, ExecutorService operationExecutor, ExecutorService coordinationExecutor, 
			                         Subscriber subscriber, FlowStats stats) {
		return new DefaultFlowContext(flowId, operationExecutor, coordinationExecutor, subscriber, stats);
	}

	private final FlowStats stats;
	private final ExecutorService operationExecutor;
	private final ExecutorService coordinationExecutor;
	private final Map<Integer, NodeState> states = new HashMap<>();
	private final Subscriber subscriber;
	private final long flowId;
	private final long started;

	private volatile boolean done = false;

	private final boolean statsEnabled;

	private volatile long threadId = -1; // only used when asserts are on

	private DefaultFlowContext(long flowId, ExecutorService operationExecutor,
			ExecutorService coordinationExecutor, Subscriber subscriber,
			FlowStats stats) {
		this.operationExecutor = operationExecutor;
		this.coordinationExecutor = coordinationExecutor;
		this.subscriber = subscriber;
		this.flowId = flowId;
		this.stats = stats;
		this.statsEnabled = stats != null;
		started = System.nanoTime();
		if (statsEnabled) stats.requests.increment();
		assert calculateThreadId();

	}

	@Override
	public long flowId() {
		return flowId;
	}
	
	@Override
	public long contextId() {
		return contextId;
	}

	@Override
	public long started() {
		return started;
	}

	@Override
	public ExecutorService operationExecutor() {
		return operationExecutor;
	}

	@Override
	public ExecutorService coordinationExecutor() {
		return coordinationExecutor;
	}

	@Override
	public void handleAction(ActionHandler next, ErrorHandler error, MutableData data) {
		coordinationExecutor.execute(() -> {
			assert !done : "stop calling me, we're done!";
			try {
				next.call(data, this);
			} catch (Throwable t) {
				error.error(data, this, t);
			}
		});
	}


	@Override
	public void handleHalted(HaltedHandler halted) {
		coordinationExecutor.execute(() -> {
			halted.halted(this);
		});
	}

	@Override
	public void handleError(ErrorHandler error, Data data, Throwable t) {
		coordinationExecutor.execute(() -> {
			error.error(data, this, t);
		});
	}
	
	@Override
	public NodeState stateFor(int id) {
		assert hasCorrectThread() : "wrong thread " + Thread.currentThread().getId() + " vs " + threadId;
		NodeState state = states.get(id);
		if (state == null) {
			state = DefaultNodeState.get();
			states.put(id, state);
		}
		return state;
	}

	@Override
	public void end(MutableData data) {
		assert hasCorrectThread() : "wrong thread " + Thread.currentThread().getId() + " vs " + threadId;
		done = true;
		subscriber.ok(data);
		if (statsEnabled) stats.completed.increment();
	}

	@Override
	public void error(Data data, Throwable t) {
		assert hasCorrectThread() : "wrong thread " + Thread.currentThread().getId() + " vs " + threadId;
		done = true;
		subscriber.error(data, t);
		if (statsEnabled) stats.errors.increment();
	}

	@Override
	public void halted() {
		assert hasCorrectThread();
		done = true;
		subscriber.halted();
		if (statsEnabled) stats.halts.increment();
	}

	@Override
	public boolean statsEnabled() {
		return statsEnabled;
	}

	private boolean hasCorrectThread() {
		return Thread.currentThread().getId() == threadId;
	}

	private boolean calculateThreadId() {
		CountDownLatch latch = new CountDownLatch(1);
		coordinationExecutor.execute(() -> {
			threadId = Thread.currentThread().getId();
			latch.countDown();
		});
		try {
			if (Thread.currentThread().getId() != threadId)	latch.await();
		} catch (InterruptedException e) {
			throw unchecked(e);
		}
		return true;
	}

}