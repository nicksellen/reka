package reka.exec;

import static reka.api.Path.dots;
import static reka.api.Path.path;
import reka.api.Path;
import reka.config.configurer.annotations.Conf;
import reka.core.setup.OperationConfigurer;
import reka.core.setup.OperationSetup;
import reka.exec.ExecConfigurer.ExecScripts;

public class ExecCommandConfigurer implements OperationConfigurer {
	
	private final ExecScripts scripts;
	private final java.nio.file.Path tmp;
	
	private Path into = path("result");
	
	public ExecCommandConfigurer(ExecScripts scripts, java.nio.file.Path tmp) {
		this.scripts = scripts;
		this.tmp = tmp;
	}
	
	@Conf.Val
	public void into(String val) {
		into = dots(val);
	}
	

	@Override
	public void setup(OperationSetup ops) {
		ops.add("run", () -> new ExecCommandOperation(scripts, tmp, into));
	}

}
