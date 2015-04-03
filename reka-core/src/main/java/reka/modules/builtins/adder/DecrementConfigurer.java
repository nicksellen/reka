package reka.modules.builtins.adder;

import reka.module.setup.OperationConfigurer;
import reka.module.setup.OperationSetup;

public class DecrementConfigurer implements OperationConfigurer {

	@Override
	public void setup(OperationSetup ops) {
		ops.add("dec", () -> new DecrementOperation(ops.ctx().get(AdderConfigurer.ADDER)));
	}

}
