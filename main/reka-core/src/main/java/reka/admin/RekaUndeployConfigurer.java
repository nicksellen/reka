package reka.admin;

import java.util.function.Function;

import reka.ApplicationManager;
import reka.api.data.Data;
import reka.config.configurer.annotations.Conf;
import reka.core.setup.OperationSetup;
import reka.core.util.StringWithVars;
import reka.nashorn.OperationConfigurer;

public class RekaUndeployConfigurer implements OperationConfigurer {

	private final ApplicationManager manager;
	private Function<Data,String> identityFn;
	
	RekaUndeployConfigurer(ApplicationManager manager) {
		this.manager = manager;
	}
	
	@Conf.Val
	@Conf.At("identity")
	public void in(String val) {
		identityFn = StringWithVars.compile(val);
	}
	
	@Override
	public void setup(OperationSetup ops) {
		ops.add("deploy", store -> new RekaUndeployOperation(manager, identityFn));
	}
	
}