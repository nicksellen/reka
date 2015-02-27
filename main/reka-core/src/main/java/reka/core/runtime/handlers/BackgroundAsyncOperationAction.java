package reka.core.runtime.handlers;

import java.util.concurrent.ExecutorService;

import reka.api.data.MutableData;
import reka.api.run.AsyncOperation;
import reka.api.run.AsyncOperation.OperationResult;
import reka.api.run.OperationContext;
import reka.core.runtime.FlowContext;

public class BackgroundAsyncOperationAction implements ActionHandler {

	private final AsyncOperation op;
	private final ActionHandler next;
	private final ErrorHandler error;
	private final ExecutorService backgroundExecutor;
	
	public BackgroundAsyncOperationAction(AsyncOperation op, ActionHandler next, ErrorHandler error, ExecutorService backgroundExecutor) {
		this.op = op;
		this.next = next;
		this.error = error;
		this.backgroundExecutor = backgroundExecutor;
	}
	
	@Override
	public void call(MutableData data, FlowContext context) {
		backgroundExecutor.execute(() -> {
			try {
				// TODO: don't create new one each time
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
			} catch (Throwable t) {
				context.handleError(error, data, t);
			}
		});
	}

}
