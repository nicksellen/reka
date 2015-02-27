package reka.builtins;

import java.util.function.Function;

import reka.api.data.Data;
import reka.api.data.MutableData;
import reka.api.run.Operation;
import reka.api.run.OperationContext;

public class ThrowOperation implements Operation {
	
	private final Function<Data,String> msgFn;
	
	public ThrowOperation(Function<Data,String> msgFn) {
		this.msgFn = msgFn;
	}

	@Override
	public void call(MutableData data, OperationContext ctx) {
		throw new RuntimeException(msgFn.apply(data));
	}
	
}