package reka.builtins.adder;

import static reka.api.Path.dots;
import static reka.api.Path.path;
import reka.api.Path;
import reka.config.configurer.annotations.Conf;
import reka.core.setup.OperationConfigurer;
import reka.core.setup.OperationSetup;

public class SumConfigurer implements OperationConfigurer {

	private Path into = path("sum");
	
	@Conf.Val
	public void into(String val) {
		into = dots(val);
	}
	
	@Override
	public void setup(OperationSetup ops) {
		ops.add("sum", ctx -> new SumOperation(ctx.get(AdderConfigurer.ADDER), into));
	}

}
