package reka.core.runtime.handlers;

import reka.api.data.MutableData;
import reka.api.run.AsyncOperation;
import reka.api.run.AsyncOperation.OperationResult;
import reka.core.runtime.FlowContext;

public class AsyncOperationAction implements ActionHandler {

	private final AsyncOperation op;
	private final ActionHandler next;
	private final ErrorHandler error;
	
	public AsyncOperationAction(AsyncOperation op, ActionHandler next, ErrorHandler error) {
		this.op = op;
		this.next = next;
		this.error = error;
	}
	
	@Override
	public void call(MutableData data, FlowContext context) {
		op.call(data, new OperationResult(){

			@Override
			public void done() {
				context.call(next, error, data);
			}

			@Override
			public void error(Throwable t) {
				error.error(data, context, t);
			}
			
		});
	}

}
