package reka.admin;

import java.util.UUID;
import java.util.function.Function;

import reka.ApplicationManager;
import reka.AppDirs;
import reka.api.data.Data;
import reka.config.configurer.annotations.Conf;
import reka.core.setup.OperationConfigurer;
import reka.core.setup.OperationSetup;
import reka.core.util.StringWithVars;

public class RekaDeployConfigurer implements OperationConfigurer {

	private final ApplicationManager manager;
	private final AppDirs dirs;
	
	private Function<Data,String> filenameFn;
	private Function<Data,String> identityFn = (unused) -> UUID.randomUUID().toString();
	
	RekaDeployConfigurer(ApplicationManager manager, AppDirs dirs) {
		this.manager = manager;
		this.dirs = dirs;
	}
	
	@Conf.At("filename")
	public void filename(String val) {
		filenameFn = StringWithVars.compile(val);
	}
	
	@Conf.At("identity")
	public void identity(String val) {
		identityFn = StringWithVars.compile(val);
	}
	
	@Override
	public void setup(OperationSetup ops) {
		if (filenameFn != null) {
			ops.add("deploy", store -> new RekaDeployFromFileOperation(manager, filenameFn, identityFn));
		} else {
			ops.add("deploy", store -> new RekaDeployFromUnpackOperation(manager, dirs.basedirs(), identityFn));
		}
	}
	
}