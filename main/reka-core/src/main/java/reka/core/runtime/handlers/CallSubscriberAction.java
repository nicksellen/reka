package reka.core.runtime.handlers;

import reka.api.data.MutableData;
import reka.core.runtime.FlowContext;

final class CallSubscriberAction implements ActionHandler {
	
	private final ActionHandler next;
	
	public CallSubscriberAction(ActionHandler next) {
		this.next = next;
	}
	
	@Override
	public void call(MutableData data, FlowContext context) {
		context.subscriber().ok(data);
		next.call(data, context);
	}
	
}