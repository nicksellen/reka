package reka.exec;

import static reka.api.Path.dots;
import static reka.api.Path.path;
import reka.api.Path;
import reka.config.configurer.annotations.Conf;
import reka.core.setup.OperationConfigurer;
import reka.core.setup.OperationSetup;
import reka.exec.ExecConfigurer.ExecScripts;

public class ExecSshCommandConfigurer implements OperationConfigurer {
	
	private final ExecScripts scripts;
	
	private Path into = path("result");
	
	public ExecSshCommandConfigurer(ExecScripts scripts) {
		this.scripts = scripts;
	}
	
	@Conf.Val
	public void into(String val) {
		into = dots(val);
	}
	
	@Override
	public void setup(OperationSetup ops) {
		ops.add("ssh run", () -> new ExecSshCommandOperation(scripts, ops.ctx().require(ExecConfigurer.CLIENT), into));
	}

}
