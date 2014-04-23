package reka.core.builder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.api.data.MutableData;
import reka.core.runtime.FlowContext;
import reka.core.runtime.handlers.ActionHandler;

public class TimeLoggerAction implements ActionHandler {
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	private final int nodeId;
	private final ActionHandler next;
	
	public TimeLoggerAction(int nodeId, ActionHandler next) {
		this.nodeId = nodeId;
		this.next = next;
	}

	@Override
	public void call(MutableData data, FlowContext context) {
		
		log.debug("f:{} c:{} t:{} n:{} > {} us", 
			context.flowId(), 
			context.id(), 
			context.threadId(), 
			nodeId, 
			Math.round((System.nanoTime() - context.started()) / 1E3));
		
		next.call(data, context);
	}

}
