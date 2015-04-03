package reka.modules.builtins.adder;

import java.util.concurrent.atomic.LongAdder;

import reka.data.MutableData;
import reka.flow.ops.Operation;
import reka.flow.ops.OperationContext;

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
