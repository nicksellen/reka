package reka.admin;

import java.util.function.Function;

import reka.Identity;
import reka.api.Path;
import reka.api.data.Data;
import reka.api.data.MutableData;
import reka.api.run.Operation;
import reka.api.run.OperationContext;
import reka.core.app.manager.ApplicationManager;

public class RekaDetailsOperation implements Operation {
	
	private final ApplicationManager manager;
	private final Path out;
	private final Function<Data,Path> appPathFn;
	
	public RekaDetailsOperation(ApplicationManager manager, Function<Data,Path> appPathFn, Path out) {
		this.manager = manager;
		this.appPathFn = appPathFn;
		this.out = out;
	}

	@Override
	public void call(MutableData data, OperationContext ctx) {
		Identity identity = manager.identityFor(appPathFn.apply(data));
		manager.get(identity).ifPresent(app -> 
			AdminUtils.putAppDetails(data.createMapAt(out), app, manager.statusFor(identity)));
	}

}
