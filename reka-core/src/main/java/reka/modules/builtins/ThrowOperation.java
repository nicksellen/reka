package reka.modules.builtins;

import java.util.function.Function;

import reka.data.Data;
import reka.data.MutableData;
import reka.flow.ops.Operation;
import reka.flow.ops.OperationContext;

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