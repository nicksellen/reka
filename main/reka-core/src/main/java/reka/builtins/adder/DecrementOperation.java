package reka.builtins.adder;

import java.util.concurrent.atomic.LongAdder;

import reka.api.data.MutableData;
import reka.api.run.Operation;
import reka.api.run.OperationContext;

public class DecrementOperation implements Operation {

	private final LongAdder adder;
	
	public DecrementOperation(LongAdder counter) {
		this.adder = counter;
	}
	
	@Override
	public void call(MutableData data, OperationContext ctx) {
		adder.decrement();
	}

}
