package reka.admin;

import static reka.api.Path.dots;
import static reka.util.Util.runtime;

import java.util.UUID;
import java.util.function.Function;

import reka.ApplicationManager;
import reka.api.Path;
import reka.api.data.Data;
import reka.config.configurer.annotations.Conf;
import reka.core.bundle.OperationSetup;
import reka.core.util.StringWithVars;
import reka.nashorn.OperationsConfigurer;

public class RekaDeployConfigurer implements OperationsConfigurer {

	private final ApplicationManager manager;
	
	private Path in;
	private Function<Data,String> filenameFn;
	private Function<Data,String> identityFn = (unused) -> UUID.randomUUID().toString();
	
	RekaDeployConfigurer(ApplicationManager manager) {
		this.manager = manager;
	}
	
	@Conf.At("in")
	public void in(String val) {
		in = dots(val);
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
		if (in != null) {
			ops.add("deploy", store -> new RekaDeployFromContentOperation(manager, in));
		} else if (filenameFn != null) {
			ops.add("deploy", store -> new RekaDeployFromFileOperation(manager, filenameFn, identityFn));
		} else {
			throw runtime("must specify either 'in' or 'filename'");
		}
	}
	
}