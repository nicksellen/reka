package reka.core.runtime.handlers;

import reka.api.data.MutableData;
import reka.api.run.AsyncOperation;
import reka.api.run.AsyncOperation.OperationResult;
import reka.api.run.OperationContext;
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
		context.operationExecutor().execute(() -> {
			// TODO: don't create a new one each time
			op.call(data, new OperationContext(context.store()), new OperationResult(){
	
				@Override
				public void done() {
					context.handleAction(next, error, data);
				}
	
				@Override
				public void error(Throwable t) {
					context.handleError(error, data, t);
				}
				
			});
		});
	}

}