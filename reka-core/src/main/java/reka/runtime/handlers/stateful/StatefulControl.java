package reka.runtime.handlers.stateful;

import java.util.Iterator;

import reka.data.MutableData;
import reka.runtime.FlowContext;
import reka.runtime.handlers.ActionHandler;
import reka.runtime.handlers.ControlHandler;
import reka.runtime.handlers.ErrorHandler;
import reka.runtime.handlers.HaltedHandler;

public class StatefulControl implements ControlHandler {
	
	private final int id;
	private final int initialCount;
	private final ActionHandler next;
	private final HaltedHandler halt;
	private final ErrorHandler error;
	
	public StatefulControl(int id, int initialCount, ActionHandler next, HaltedHandler halt, ErrorHandler error) {
		this.id = id;
		this.initialCount = initialCount;
		this.next = next;
		this.halt = halt;
		this.error = error;
	}

	@Override
	public void call(MutableData data, FlowContext context) {
		NodeState state = stateFor(context);
		
		state.arrived(data);

		state.decrement();
		
		switch (state.lifecycle()) {
		case ACTIVE:
		case ALWAYS_ACTIVE:
			run(context);
		default:
			break;
		}
	}

	@Override
	public void halted(FlowContext context) {
		NodeState state = stateFor(context);
		
		state.decrement();
		
		switch (state.lifecycle()) {
		case ACTIVE:
		case ALWAYS_ACTIVE:
			run(context);
			break;
		case INACTIVE:
			halt.halted(context);
			break;
		case WAITING:
			break;
		}
	}

	private NodeState stateFor(FlowContext context) {
		return context.stateFor(id).initialize(initialCount);
	}
	
	private void run(final FlowContext context) {
		
		NodeState state = stateFor(context);
		
		Iterator<MutableData> it = state.data().iterator();

		MutableData mergedData = it.next();
		
		while (it.hasNext()) {
			mergedData.merge(it.next());
		}
		
		context.handleAction(next, error, mergedData);
	}


}
