package reka.flow.builder;

import reka.data.Data;
import reka.data.MutableData;
import reka.flow.Flow;
import reka.flow.ops.Subscriber;
import reka.runtime.FlowContext;
import reka.runtime.handlers.ActionHandler;
import reka.runtime.handlers.ErrorHandler;
import reka.runtime.handlers.HaltedHandler;

public class EmbeddedFlowAction implements ActionHandler {

	private final Flow flow;
	private final ActionHandler next;
	private final HaltedHandler halted;
	private final ErrorHandler error;
	
	public EmbeddedFlowAction(Flow flow, ActionHandler next, HaltedHandler halted, ErrorHandler error) {
		this.flow = flow;
		this.next = next;
		this.halted = halted;
		this.error = error;
	}
	
	@Override
	public void call(MutableData data, FlowContext context) {
		flow.run(context.operationExecutor(), context.coordinationExecutor() , data, new Subscriber() {
				
			@Override
			public void ok(MutableData data) {
				context.handleAction(next, error, data);
			}
			
			@Override
			public void halted() {
				context.handleHalted(halted);
			}
			
			@Override
			public void error(Data data, Throwable t) {
				context.handleError(error, data, t);
			}
			
		}, context.store(), context.statsEnabled());	
	}

}
