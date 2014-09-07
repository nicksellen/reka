package reka.admin;

import java.util.function.Function;

import reka.ApplicationManager;
import reka.api.data.Data;
import reka.api.data.MutableData;
import reka.api.run.Operation;

public class RekaUndeployOperation implements Operation {
	
	private final ApplicationManager manager;
	private final Function<Data,String> identityFn;
	
	public RekaUndeployOperation(ApplicationManager manager, Function<Data,String> identityFn) {
		this.manager = manager;
		this.identityFn = identityFn;
	}
	
	@Override
	public MutableData call(MutableData data) {
		String identity = identityFn.apply(data);
		manager.undeploy(identity);
		return data;
	}
}