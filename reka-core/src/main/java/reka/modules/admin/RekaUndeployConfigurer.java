package reka.modules.admin;

import java.util.function.Function;

import reka.api.Path;
import reka.app.manager.ApplicationManager;
import reka.config.configurer.annotations.Conf;
import reka.data.Data;
import reka.module.setup.OperationConfigurer;
import reka.module.setup.OperationSetup;
import reka.util.StringWithVars;
import reka.util.dirs.AppDirs;

public class RekaUndeployConfigurer implements OperationConfigurer {

	private final ApplicationManager manager;
	private final AppDirs dirs;
	private Function<Data,Path> appPathFn;
	
	RekaUndeployConfigurer(ApplicationManager manager, AppDirs dirs) {
		this.manager = manager;
		this.dirs = dirs;
	}
	
	@Conf.Val
	@Conf.At("identity")
	public void in(String val) {
		appPathFn = StringWithVars.compile(val).andThen(Path::slashes);
	}
	
	@Override
	public void setup(OperationSetup ops) {
		ops.add("undeploy", () -> new RekaUndeployOperation(manager, dirs, appPathFn));
	}
	
}