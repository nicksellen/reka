package reka.exec;

import static reka.api.Path.dots;
import static reka.api.Path.path;
import reka.api.Path;
import reka.config.configurer.annotations.Conf;
import reka.core.setup.OperationConfigurer;
import reka.core.setup.OperationSetup;

public class ExecCommandConfigurer implements OperationConfigurer {
	
	private final String[] command;
	
	private Path into = path("result");
	
	public ExecCommandConfigurer(String[] command) {
		this.command = command;
	}
	
	@Conf.Val
	public void into(String val) {
		into = dots(val);
	}
	

	@Override
	public void setup(OperationSetup ops) {
		ops.add("run", ctx -> new ExecCommandOperation(command, into));
	}

}
