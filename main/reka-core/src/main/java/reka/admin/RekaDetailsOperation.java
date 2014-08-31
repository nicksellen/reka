package reka.admin;

import java.util.Optional;
import java.util.function.Function;

import reka.Application;
import reka.ApplicationManager;
import reka.api.Path;
import reka.api.data.Data;
import reka.api.data.MutableData;
import reka.api.run.SyncOperation;

public class RekaDetailsOperation implements SyncOperation {
	
	private final ApplicationManager manager;
	private final Path out;
	private final Function<Data,String> idFn;
	
	public RekaDetailsOperation(ApplicationManager manager, Function<Data,String> idFn, Path out) {
		this.manager = manager;
		this.idFn = idFn;
		this.out = out;
	}

	@Override
	public MutableData call(MutableData data) {
		String identity = idFn.apply(data);
		Optional<Application> opt = manager.get(identity);
		if (!opt.isPresent()) return data;
		AdminUtils.putAppDetails(data.createMapAt(out), opt.get());
		return data;
	}

}
