package reka.exec;

import static reka.util.Path.dots;
import static reka.util.Path.path;
import reka.config.configurer.annotations.Conf;
import reka.module.setup.OperationConfigurer;
import reka.module.setup.OperationSetup;
import reka.util.Path;

public class ExecCommandConfigurer implements OperationConfigurer {
	
	private Path into = path("result");
	
	@Conf.Val
	public void into(String val) {
		into = dots(val);
	}
	

	@Override
	public void setup(OperationSetup ops) {
		ops.add("run", () -> new ExecCommandOperation(ops.ctx().require(ExecConfigurer.COMMAND), into));
	}

}
