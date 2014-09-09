package reka.api.run;

import java.util.function.BiConsumer;

import reka.api.data.MutableData;
import reka.api.flow.SimpleFlowOperation;

public interface AsyncOperation extends SimpleFlowOperation {
	
	public static AsyncOperation create(BiConsumer<MutableData,OperationResult> c) {
		return new AsyncOperation(){

			@Override
			public void run(MutableData data, OperationResult ctx) {
				c.accept(data, ctx);
			}
			
		};
	}

	public void run(MutableData data, OperationResult ctx);
	
	public static interface OperationResult {
		void done();
		void error(Throwable t);
	}
	
}
