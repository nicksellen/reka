package reka.core.runtime.handlers;

import reka.api.data.MutableData;
import reka.api.run.Operation;
import reka.core.runtime.FlowContext;

public class OperationAction implements ActionHandler {

	private final Operation operation;
	private final ActionHandler next;
	
	public OperationAction(Operation operation, ActionHandler next) {
		this.operation = operation;
		this.next = next;
	}
	
	@Override
	public void call(MutableData data, FlowContext context) {
		operation.call(data);
		next.call(data, context);
	}

}
