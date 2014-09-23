package reka.admin;

import static reka.api.Path.path;
import reka.ApplicationManager;
import reka.core.bundle.BundleConfigurer;

public class RekaSystemBundle implements BundleConfigurer {
	
	private final ApplicationManager manager;
	
	public RekaSystemBundle(ApplicationManager manager) {
		this.manager = manager;
	}

	@Override
	public void setup(BundleSetup bundle) {
		bundle.module(path("reka"), "0.1.0", () -> new RekaModule(manager));
	}

}
