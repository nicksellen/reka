package reka.admin;

import java.util.function.Function;

import reka.api.Path;
import reka.api.data.Data;
import reka.config.configurer.annotations.Conf;
import reka.core.app.manager.ApplicationManager;
import reka.core.setup.OperationConfigurer;
import reka.core.setup.OperationSetup;
import reka.core.util.StringWithVars;
import reka.dirs.AppDirs;

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
		ops.add("undeploy", ctx -> new RekaUndeployOperation(manager, dirs, appPathFn));
	}
	
}