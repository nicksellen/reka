package reka.modules.builtins.adder;

import static reka.util.Path.dots;
import static reka.util.Path.path;
import reka.config.configurer.annotations.Conf;
import reka.module.setup.OperationConfigurer;
import reka.module.setup.OperationSetup;
import reka.util.Path;

public class SumConfigurer implements OperationConfigurer {

	private Path into = path("sum");
	
	@Conf.Val
	public void into(String val) {
		into = dots(val);
	}
	
	@Override
	public void setup(OperationSetup ops) {
		ops.add("sum", () -> new SumOperation(ops.ctx().get(AdderConfigurer.ADDER), into));
	}

}
