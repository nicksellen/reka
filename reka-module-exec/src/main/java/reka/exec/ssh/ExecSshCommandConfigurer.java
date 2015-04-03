package reka.exec.ssh;

import static reka.util.Path.dots;
import static reka.util.Path.path;
import reka.config.configurer.annotations.Conf;
import reka.exec.ExecConfigurer;
import reka.module.setup.OperationConfigurer;
import reka.module.setup.OperationSetup;
import reka.util.Path;

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
