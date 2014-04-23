package reka.core.runtime.handlers.stateful;

import java.util.Iterator;

import reka.api.data.MutableData;
import reka.core.runtime.FlowContext;
import reka.core.runtime.handlers.ActionHandler;
import reka.core.runtime.handlers.ControlHandler;
import reka.core.runtime.handlers.ErrorHandler;
import reka.core.runtime.handlers.HaltedHandler;

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
			run(context, data);
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
			run(context, null);
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
	
	private void run(final FlowContext context, MutableData incomingData) {
		
		NodeState state = stateFor(context);
		
		Iterator<MutableData> it = state.data().iterator();

		MutableData mergedData = it.next();
		
		while (it.hasNext()) {
			mergedData.merge(it.next());
		}

		if (incomingData != null) { // might be triggered from a halted call
			mergedData.merge(incomingData);
		}

		try {
			next.call(mergedData, context);
		} catch (Throwable t) {
			error.error(mergedData, context, t);
		}
	}


}
