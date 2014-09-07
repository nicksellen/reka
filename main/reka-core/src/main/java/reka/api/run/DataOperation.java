package reka.api.run;

import reka.api.data.MutableData;
import reka.api.flow.SimpleFlowOperation;

public interface DataOperation extends SimpleFlowOperation {

	public void run(MutableData data, OperationContext ctx);
	
	public static interface OperationContext {
		
		//void emit(Data data);
		void end();
		void error(Throwable t);
		
		/*
		default void end(Data data) {
			emit(data);
			end();
		}
		*/
		
	}
	
}
