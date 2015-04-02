package reka.exec;

import static reka.api.Path.dots;
import static reka.api.Path.path;
import reka.api.Path;
import reka.config.configurer.annotations.Conf;
import reka.core.setup.OperationConfigurer;
import reka.core.setup.OperationSetup;

public class ExecSshCommandConfigurer implements OperationConfigurer {
	
	private Path into = path("result");
	
	public ExecSshCommandConfigurer() {}
	
	@Conf.Val
	public void into(String val) {
		into = dots(val);
	}
	
	@Override
	public void setup(OperationSetup ops) {
		ops.add("ssh run", () -> new ExecSshCommandOperation(ops.ctx().require(ExecConfigurer.COMMAND), ops.ctx().require(ExecConfigurer.CLIENT), into));
	}

}
