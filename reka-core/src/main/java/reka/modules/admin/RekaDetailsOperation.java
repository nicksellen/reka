package reka.modules.admin;

import java.util.function.Function;

import reka.api.Path;
import reka.app.manager.ApplicationManager;
import reka.data.Data;
import reka.data.MutableData;
import reka.flow.ops.Operation;
import reka.flow.ops.OperationContext;
import reka.util.Identity;

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
