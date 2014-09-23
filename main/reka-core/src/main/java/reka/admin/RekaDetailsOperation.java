package reka.admin;

import java.util.function.Function;

import reka.ApplicationManager;
import reka.api.Path;
import reka.api.data.Data;
import reka.api.data.MutableData;
import reka.api.run.Operation;

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
	public void call(MutableData data) {
		String identity = idFn.apply(data);
		manager.get(identity).ifPresent(app -> 
			AdminUtils.putAppDetails(data.createMapAt(out), app, manager.statusFor(identity)));
	}

}
