package reka.admin;

import java.util.function.Function;

import reka.AppDirs;
import reka.ApplicationManager;
import reka.BaseDirs;
import reka.api.data.Data;
import reka.api.data.MutableData;
import reka.api.run.Operation;

public class RekaUndeployOperation implements Operation {
	
	private final ApplicationManager manager;
	private final BaseDirs dirs;
	private final Function<Data,String> identityFn;
	
	public RekaUndeployOperation(ApplicationManager manager, AppDirs dirs, Function<Data,String> identityFn) {
		this.manager = manager;
		this.dirs = dirs.basedirs();
		this.identityFn = identityFn;
	}
	
	@Override
	public void call(MutableData data) {
		String identity = identityFn.apply(data);
		dirs.resolve(identity).delete();
		manager.undeploy(identity);
	}
	
}