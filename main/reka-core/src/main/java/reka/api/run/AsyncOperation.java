package reka.api.run;

import java.util.function.BiConsumer;

import reka.api.data.MutableData;
import reka.api.flow.SimpleFlowOperation;

public interface AsyncOperation extends SimpleFlowOperation {
	
	public static AsyncOperation create(BiConsumer<MutableData,OperationContext> c) {
		return new AsyncOperation(){

			@Override
			public void run(MutableData data, OperationContext ctx) {
				c.accept(data, ctx);
			}
		};
	}

	public void run(MutableData data, OperationContext ctx);
	
	public static interface OperationContext {
		void end();
		void error(Throwable t);
	}
	
}
