package reka.admin;

import static reka.api.Path.dots;
import static reka.api.Path.slashes;

import java.util.UUID;
import java.util.function.Function;

import reka.api.Path;
import reka.api.data.Data;
import reka.config.configurer.annotations.Conf;
import reka.core.app.manager.ApplicationManager;
import reka.core.setup.OperationConfigurer;
import reka.core.setup.OperationSetup;
import reka.core.util.StringWithVars;
import reka.dirs.AppDirs;

public class RekaDeployConfigurer implements OperationConfigurer {

	private final ApplicationManager manager;
	private final AppDirs dirs;
	
	private Function<Data,Path> dataPathFn;
	private Function<Data,Path> appPathFn = unused -> slashes(UUID.randomUUID().toString());
	
	RekaDeployConfigurer(ApplicationManager manager, AppDirs dirs) {
		this.manager = manager;
		this.dirs = dirs;
	}
	
	@Conf.At("id")
	public void identity(String val) {
		appPathFn = StringWithVars.compile(val).andThen(v -> slashes(v));
	}
	
	@Conf.At("data")
	public void data(String val) {
		dataPathFn = StringWithVars.compile(val).andThen(v -> dots(v));
	}
	
	@Override
	public void setup(OperationSetup ops) {
		ops.add("deploy", () -> new RekaDeployOperation(manager, dirs.basedirs(), dataPathFn, appPathFn));
	}
	
}