package reka.command;

import java.util.ArrayList;
import java.util.List;

import reka.config.Config;
import reka.configurer.annotations.Conf;
import reka.core.bundle.UseConfigurer;
import reka.core.bundle.UseInit;

public class UseCommand extends UseConfigurer {

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
	public void setup(UseInit init) {
		init.operation("", () -> new RunCommandConfigurer(exec, args));
	}

}
