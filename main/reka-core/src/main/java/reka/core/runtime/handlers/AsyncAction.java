package reka.core.runtime.handlers;

import static com.google.common.util.concurrent.Futures.addCallback;
import reka.api.data.MutableData;
import reka.api.run.AsyncOperation;
import reka.core.runtime.FlowContext;

import com.google.common.util.concurrent.FutureCallback;

public class AsyncAction implements ActionHandler {
	
	private final AsyncOperation operation;
	private final ActionHandler next;
	private final ErrorHandler error;
	
	public AsyncAction(AsyncOperation operation, ActionHandler next, ErrorHandler error) {
		this.operation = operation;
		this.next = next;
		this.error = error;
	}

	@Override
	public void call(MutableData data, FlowContext context) {
		try {
			addCallback(operation.call(data), new FutureCallback<MutableData>(){
				
				@Override
				public void onSuccess(MutableData result) {
					context.execute(() -> {
						try {
							next.call(result, context);
						} catch (Throwable t) {
							error.error(data, context, t);
						}
					});
				}
	
				@Override
				public void onFailure(Throwable t) {
					context.execute(() -> error.error(data, context, t));
				}
				
			});
		} catch (Throwable t) {
			context.execute(() -> error.error(data, context, t));
			//throw t;
		}
	}

}
