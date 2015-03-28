package reka.builtins.adder;

import reka.core.setup.OperationConfigurer;
import reka.core.setup.OperationSetup;

public class IncrementConfigurer implements OperationConfigurer {

	@Override
	public void setup(OperationSetup ops) {
		ops.add("inc", () -> new IncrementOperation(ops.ctx().get(AdderConfigurer.ADDER)));
	}

}
