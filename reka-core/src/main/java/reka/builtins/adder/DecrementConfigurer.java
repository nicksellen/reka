package reka.builtins.adder;

import reka.core.setup.OperationConfigurer;
import reka.core.setup.OperationSetup;

public class DecrementConfigurer implements OperationConfigurer {

	@Override
	public void setup(OperationSetup ops) {
		ops.add("dec", () -> new DecrementOperation(ops.ctx().get(AdderConfigurer.ADDER)));
	}

}
