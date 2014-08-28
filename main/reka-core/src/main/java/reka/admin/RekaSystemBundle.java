package reka.admin;

import static reka.api.Path.path;
import reka.ApplicationManager;
import reka.core.bundle.RekaBundle;

public class RekaSystemBundle implements RekaBundle {
	
	private final ApplicationManager manager;
	
	public RekaSystemBundle(ApplicationManager manager) {
		this.manager = manager;
	}

	@Override
	public void setup(BundleSetup bundle) {
		bundle.module(path("reka"), () -> new RekaModule(manager));
	}

}
