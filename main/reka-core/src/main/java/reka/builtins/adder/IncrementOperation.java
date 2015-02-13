package reka.builtins.adder;

import java.util.concurrent.atomic.LongAdder;

import reka.api.data.MutableData;
import reka.api.run.Operation;

public class IncrementOperation implements Operation {

	private final LongAdder adder;
	
	public IncrementOperation(LongAdder counter) {
		this.adder = counter;
	}
	
	@Override
	public void call(MutableData data) {
		adder.increment();
	}

}
