package reka.admin;

import static reka.api.Path.slashes;

import java.util.function.Function;

import reka.Identity;
import reka.api.Path;
import reka.api.data.Data;
import reka.api.data.MutableData;
import reka.api.run.Operation;
import reka.api.run.OperationContext;
import reka.core.app.manager.ApplicationManager;
import reka.dirs.AppDirs;
import reka.dirs.BaseDirs;

public class RekaUndeployOperation implements Operation {
	
	private final ApplicationManager manager;
	private final BaseDirs basedirs;
	private final Function<Data,Path> appPathFn;
	
	public RekaUndeployOperation(ApplicationManager manager, AppDirs dirs, Function<Data,Path> appPathFn) {
		this.manager = manager;
		this.basedirs = dirs.basedirs();
		this.appPathFn = appPathFn;
	}
	
	@Override
	public void call(MutableData data, OperationContext ctx) {
		Path appPath = appPathFn.apply(data);
		basedirs.delete(appPath);
		manager.undeploy(appPath);
	}
	
}