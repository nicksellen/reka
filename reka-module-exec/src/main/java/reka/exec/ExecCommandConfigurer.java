package reka.exec;

import static reka.api.Path.dots;
import static reka.api.Path.path;
import reka.api.Path;
import reka.config.configurer.annotations.Conf;
import reka.core.setup.OperationConfigurer;
import reka.core.setup.OperationSetup;

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
