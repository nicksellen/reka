package reka.core.builder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.api.data.MutableData;
import reka.core.runtime.FlowContext;
import reka.core.runtime.handlers.ActionHandler;
import reka.core.runtime.handlers.ErrorHandler;

public class InspectDataAction implements ActionHandler {
	
	private final Logger log = LoggerFactory.getLogger(getClass());

	private final String msg;
	private final ActionHandler next;
	private final ErrorHandler error;
	
	public InspectDataAction(String msg, ActionHandler next, ErrorHandler error) {
		this.msg = msg;
		this.next = next;
		this.error = error;
	}
	
	@Override
	public void call(MutableData data, FlowContext context) {
		log.info("{} : {}", msg, data.toPrettyJson());
		context.call(next, error, data);
	}

}
