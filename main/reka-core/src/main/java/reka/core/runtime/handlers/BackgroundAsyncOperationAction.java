package reka.core.runtime.handlers;

import java.util.concurrent.ExecutorService;

import reka.api.data.MutableData;
import reka.api.run.AsyncOperation;
import reka.api.run.AsyncOperation.OperationResult;
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
				op.run(data, new OperationResult(){
		
					@Override
					public void done() {
						context.call(next, error, data);
					}
		
					@Override
					public void error(Throwable t) {
						error.error(data, context, t);
					}
					
				});
			} catch (Throwable t) {
				error.error(data, context, t);
			}
		});
	}

}
