package reka.exec;

import static reka.api.Path.dots;
import static reka.api.Path.path;
import reka.api.Path;
import reka.config.configurer.annotations.Conf;
import reka.core.setup.OperationConfigurer;
import reka.core.setup.OperationSetup;

public class ExecSshCommandConfigurer implements OperationConfigurer {
	
	private final String[] command;
	private final SshConfig config;
	
	private Path into = path("result");
	
	public ExecSshCommandConfigurer(String[] command, SshConfig config) {
		this.command = command;
		this.config = config;
	}
	
	@Conf.Val
	public void into(String val) {
		into = dots(val);
	}
	

	@Override
	public void setup(OperationSetup ops) {
		ops.add("ssh run", store -> new ExecSshCommandOperation(command, config, into));
	}

}
