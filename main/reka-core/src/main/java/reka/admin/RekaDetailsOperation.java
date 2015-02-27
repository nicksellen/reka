package reka.admin;

import java.util.function.Function;

import reka.api.Path;
import reka.api.data.Data;
import reka.api.data.MutableData;
import reka.api.run.Operation;
import reka.api.run.OperationContext;
import reka.core.app.manager.ApplicationManager;

public class RekaDetailsOperation implements Operation {
	
	private final ApplicationManager manager;
	private final Path out;
	private final Function<Data,String> idFn;
	
	public RekaDetailsOperation(ApplicationManager manager, Function<Data,String> idFn, Path out) {
		this.manager = manager;
		this.idFn = idFn;
		this.out = out;
	}

	@Override
	public void call(MutableData data, OperationContext ctx) {
		String identity = idFn.apply(data);
		manager.get(identity).ifPresent(app -> 
			AdminUtils.putAppDetails(data.createMapAt(out), app, manager.statusFor(identity)));
	}

}
