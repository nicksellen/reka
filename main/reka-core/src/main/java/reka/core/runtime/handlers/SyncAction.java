package reka.core.runtime.handlers;

import reka.api.data.MutableData;
import reka.api.run.SyncOperation;
import reka.core.runtime.FlowContext;

public class SyncAction implements ActionHandler {

	private final SyncOperation operation;
	private final ActionHandler next;
	
	public SyncAction(SyncOperation operation, ActionHandler next) {
		this.operation = operation;
		this.next = next;
	}
	
	@Override
	public void call(MutableData data, FlowContext context) {
		next.call(operation.call(data), context);
	}

}
