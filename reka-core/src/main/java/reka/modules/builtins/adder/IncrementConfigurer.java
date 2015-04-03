package reka.modules.builtins.adder;

import reka.module.setup.OperationConfigurer;
import reka.module.setup.OperationSetup;

public class IncrementConfigurer implements OperationConfigurer {

	@Override
	public void setup(OperationSetup ops) {
		ops.add("inc", () -> new IncrementOperation(ops.ctx().get(AdderConfigurer.ADDER)));
	}

}
