package reka.builtins.adder;

import java.util.concurrent.atomic.LongAdder;

import reka.api.data.MutableData;
import reka.api.run.Operation;

public class DecrementOperation implements Operation {

	private final LongAdder adder;
	
	public DecrementOperation(LongAdder counter) {
		this.adder = counter;
	}
	
	@Override
	public void call(MutableData data) {
		adder.decrement();
	}

}
