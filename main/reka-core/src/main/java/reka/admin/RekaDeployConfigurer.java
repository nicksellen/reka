package reka.admin;

import static reka.util.Util.runtime;

import java.util.UUID;
import java.util.function.Function;

import reka.ApplicationManager;
import reka.api.data.Data;
import reka.config.configurer.annotations.Conf;
import reka.core.setup.OperationSetup;
import reka.core.util.StringWithVars;
import reka.nashorn.OperationConfigurer;

public class RekaDeployConfigurer implements OperationConfigurer {

	private final ApplicationManager manager;
	
	private Function<Data,String> filenameFn;
	private Function<Data,String> identityFn = (unused) -> UUID.randomUUID().toString();
	
	RekaDeployConfigurer(ApplicationManager manager) {
		this.manager = manager;
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
			throw runtime("must specify either 'in' or 'filename'");
		}
	}
	
}