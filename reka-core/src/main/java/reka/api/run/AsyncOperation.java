package reka.api.run;

import static reka.util.Util.runtime;
import reka.api.data.MutableData;
import reka.api.flow.SimpleFlowOperation;
import reka.util.TriConsumer;

public interface AsyncOperation extends SimpleFlowOperation {
	
	public static AsyncOperation create(TriConsumer<MutableData,OperationContext,OperationResult> c) {
		return new AsyncOperation(){

			@Override
			public void call(MutableData data, OperationContext ctx, OperationResult res) {
				c.accept(data, ctx, res);
			}
			
		};
	}

	public void call(MutableData data, OperationContext ctx, OperationResult res);
	
	public static interface OperationResult {
		
		void done();
		void error(Throwable t);
		
		default void error(String msg, Object... objs) {
			error(runtime(msg, objs));
		}
		
	}
	
}
