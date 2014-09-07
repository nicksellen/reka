package reka.admin;

import java.util.function.Function;

import reka.ApplicationManager;
import reka.api.data.Data;
import reka.config.configurer.annotations.Conf;
import reka.core.bundle.OperationSetup;
import reka.core.util.StringWithVars;
import reka.nashorn.OperationsConfigurer;

public class RekaRedeployConfigurer implements OperationsConfigurer {

	private final ApplicationManager manager;
	private Function<Data,String> identityFn;
	
	RekaRedeployConfigurer(ApplicationManager manager) {
		this.manager = manager;
	}
	
	@Conf.Val
	@Conf.At("identity")
	public void in(String val) {
		identityFn = StringWithVars.compile(val);
	}
	
	@Override
	public void setup(OperationSetup ops) {
		ops.add("redeploy", store -> new RekaRedeployOperation(manager, identityFn));
	}
	
}