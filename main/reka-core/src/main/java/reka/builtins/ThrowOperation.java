package reka.builtins;

import java.util.function.Function;

import reka.api.data.Data;
import reka.api.data.MutableData;
import reka.api.run.Operation;

public class ThrowOperation implements Operation {
	
	private final Function<Data,String> msgFn;
	
	public ThrowOperation(Function<Data,String> msgFn) {
		this.msgFn = msgFn;
	}

	@Override
	public void call(MutableData data) {
		throw new RuntimeException(msgFn.apply(data));
	}
	
}