package reka.modules.builtins.adder;

import java.util.concurrent.atomic.LongAdder;

import reka.data.MutableData;
import reka.flow.ops.Operation;
import reka.flow.ops.OperationContext;
import reka.util.Path;

public class SumOperation implements Operation {

	private final LongAdder adder;
	private final Path into;
	
	public SumOperation(LongAdder counter, Path into) {
		this.adder = counter;
		this.into = into;
	}
	
	@Override
	public void call(MutableData data, OperationContext ctx) {
		data.putLong(into, adder.sum());
	}

}
