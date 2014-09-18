package reka.core.runtime;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import reka.api.data.Data;
import reka.api.data.MutableData;
import reka.api.run.Subscriber;
import reka.core.runtime.handlers.ActionHandler;
import reka.core.runtime.handlers.ErrorHandler;
import reka.core.runtime.handlers.stateful.NodeState;
import reka.core.runtime.handlers.stateful.PooledNodeState;
import reka.util.Recycler;
import reka.util.Recycler.Handle;

public class PooledFlowContext implements FlowContext {
	
	private static final Recycler<PooledFlowContext> RECYCLER = new Recycler<PooledFlowContext>() {
		
		@Override
		protected PooledFlowContext newObject(Handle handle) {
			return new PooledFlowContext(handle);
		}
		
	};
	
	public static PooledFlowContext get(long flowId, ExecutorService executor, Subscriber subscriber) {
		PooledFlowContext ctx = RECYCLER.get();
		ctx.executor = executor;
		ctx.subscriber = subscriber;
		ctx.flowId = flowId;
		ctx.started = System.nanoTime();
		return ctx;
	}

	private final Map<Integer,PooledNodeState> states = new HashMap<>();

	private final Handle handle;
	
    public boolean recycle() {
    	states.values().forEach(PooledNodeState::recycle);
    	states.clear();
    	executor = null;
		subscriber = null;
		flowId = -1;
		started = -1;
        return RECYCLER.recycle(this, handle);
    }
	
	private long flowId;
	private ExecutorService executor;
	private Subscriber subscriber;
	private long started;
	
	private PooledFlowContext(Handle handle) {
		this.handle = handle;
	}

	@Override
    public NodeState stateFor(int id) {
		PooledNodeState state = states.get(id);
		if (state == null) {
			state = PooledNodeState.get();
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
	public void end(MutableData data) {
		subscriber.ok(data);
		recycle();
	}

	@Override
	public void error(Data data, Throwable t) {
		subscriber.error(data, t);
	}

	@Override
	public void halted() {
		subscriber.halted();
	}
}