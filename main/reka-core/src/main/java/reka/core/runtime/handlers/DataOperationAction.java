package reka.core.runtime.handlers;

import reka.api.data.MutableData;
import reka.api.run.DataOperation;
import reka.api.run.DataOperation.OperationContext;
import reka.core.runtime.FlowContext;

public class DataOperationAction implements ActionHandler {

	private final DataOperation op;
	private final ActionHandler next;
	private final ErrorHandler error;
	
	public DataOperationAction(DataOperation op, ActionHandler next, ErrorHandler error) {
		this.op = op;
		this.next = next;
		this.error = error;
	}
	
	@Override
	public void call(MutableData data, FlowContext context) {
		op.run(data, new OperationContext() {
			
			public void emit(MutableData data) {
				next.call(data, context);	
			}
			
			@Override
			public void end() {
				next.call(data, context);
			}

			@Override
			public void error(Throwable t) {
				error.error(data, context, t);
			}
			
		});
	}

}
