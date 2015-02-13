package reka.builtins.adder;

import java.util.concurrent.atomic.LongAdder;

import reka.api.Path;
import reka.api.data.MutableData;
import reka.api.run.Operation;

public class SumOperation implements Operation {

	private final LongAdder adder;
	private final Path into;
	
	public SumOperation(LongAdder counter, Path into) {
		this.adder = counter;
		this.into = into;
	}
	
	@Override
	public void call(MutableData data) {
		data.putLong(into, adder.sum());
	}

}
