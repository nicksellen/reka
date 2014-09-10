package reka.api.run;

import java.util.function.BiConsumer;

import reka.api.data.MutableData;
import reka.api.flow.SimpleFlowOperation;

public interface AsyncOperation extends SimpleFlowOperation {
	
	public static AsyncOperation create(BiConsumer<MutableData,OperationResult> c) {
		return new AsyncOperation(){

			@Override
			public void call(MutableData data, OperationResult res) {
				c.accept(data, res);
			}
			
		};
	}

	public void call(MutableData data, OperationResult res);
	
	public static interface OperationResult {
		void done();
		void error(Throwable t);
	}
	
}
