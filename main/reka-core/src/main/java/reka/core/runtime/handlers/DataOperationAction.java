package reka.core.runtime.handlers;

import reka.api.data.MutableData;
import reka.api.run.AsyncOperation;
import reka.api.run.AsyncOperation.OperationContext;
import reka.core.runtime.FlowContext;

public class DataOperationAction implements ActionHandler {

	private final AsyncOperation op;
	private final ActionHandler next;
	private final ErrorHandler error;
	
	public DataOperationAction(AsyncOperation op, ActionHandler next, ErrorHandler error) {
		this.op = op;
		this.next = next;
		this.error = error;
	}
	
	@Override
	public void call(MutableData data, FlowContext context) {
		op.run(data, new OperationContext(){

			@Override
			public void end() {
				context.call(next, error, data);
			}

			@Override
			public void error(Throwable t) {
				error.error(data, context, t);
			}
			
		});
	}

}
