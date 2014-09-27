package reka.command;

import static reka.api.Path.root;

import java.util.ArrayList;
import java.util.List;

import reka.config.Config;
import reka.config.configurer.annotations.Conf;
import reka.core.setup.ModuleConfigurer;
import reka.core.setup.ModuleSetup;

public class CommandConfigurer extends ModuleConfigurer {

	private final List<String> args = new ArrayList<>();
	private String exec;
	
	@Conf.At("exec")
	public void exec(String val) {
		exec = val;
	}
	
	@Conf.EachChildOf("args")
	public void arg(Config config) {
		args.add(config.key());
		if (config.hasValue()) {
			args.add(config.valueAsString());
		}
	}
	
	@Conf.Each("arg")
	public void arg(String val) {
		args.add(val);
	}
	
	@Override
	public void setup(ModuleSetup module) {
		module.operation(root(), provider -> new RunCommandConfigurer(exec, args));
	}

}
