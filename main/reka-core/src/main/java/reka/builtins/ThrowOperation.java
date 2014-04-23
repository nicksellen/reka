package reka.builtins;

import java.util.function.Function;

import reka.api.data.Data;
import reka.api.data.MutableData;
import reka.api.run.SyncOperation;

public class ThrowOperation implements SyncOperation {
	
	private final Function<Data,String> msgFn;
	
	public ThrowOperation(Function<Data,String> msgFn) {
		this.msgFn = msgFn;
	}

	@Override
	public MutableData call(MutableData data) {
		throw new RuntimeException(msgFn.apply(data));
	}
	
}