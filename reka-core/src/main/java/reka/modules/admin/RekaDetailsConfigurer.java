package reka.modules.admin;

import static reka.util.Path.dots;

import java.util.function.Function;

import reka.app.manager.ApplicationManager;
import reka.config.configurer.annotations.Conf;
import reka.data.Data;
import reka.module.setup.OperationConfigurer;
import reka.module.setup.OperationSetup;
import reka.util.Path;
import reka.util.StringWithVars;

public class RekaDetailsConfigurer implements OperationConfigurer {
	
	private final ApplicationManager manager;
	
	private Path out = dots("app");
	private Function<Data,Path> appPathFn;
	
	@Conf.Val
	@Conf.At("out")
	@Conf.At("into")
	public void out(String val) {
		out = dots(val);
	}
	
	@Conf.At("id")
	public void app(String val) {
		appPathFn = StringWithVars.compile(val).andThen(Path::slashes);
	}
	
	public RekaDetailsConfigurer(ApplicationManager manager) {
		this.manager = manager;
	}
	
	@Override
	public void setup(OperationSetup ops) {
		ops.add("get", () -> new RekaDetailsOperation(manager, appPathFn, out));
	}

}
