package reka.admin;

import java.util.function.Function;

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
	private final Function<Data,String> identityFn;
	
	public RekaUndeployOperation(ApplicationManager manager, AppDirs dirs, Function<Data,String> identityFn) {
		this.manager = manager;
		this.basedirs = dirs.basedirs();
		this.identityFn = identityFn;
	}
	
	@Override
	public void call(MutableData data, OperationContext ctx) {
		String identity = identityFn.apply(data);
		basedirs.delete(identity);
		manager.undeploy(identity);
	}
	
}