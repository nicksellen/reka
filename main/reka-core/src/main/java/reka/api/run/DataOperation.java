package reka.api.run;

import reka.api.data.Data;
import reka.api.data.MutableData;
import reka.api.flow.FlowOperation;

public interface DataOperation extends FlowOperation {

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
