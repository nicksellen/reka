package reka.admin;

import static reka.api.Path.path;
import reka.ApplicationManager;
import reka.api.Path;
import reka.core.bundle.BundleConfigurer;

public class RekaSystemBundle implements BundleConfigurer {

	@Override
	public Path base() {
		return path("reka");
	}
	
	private final ApplicationManager manager;
	
	public RekaSystemBundle(ApplicationManager manager) {
		this.manager = manager;
	}

	@Override
	public void setup(BundleSetup bundle) {
		bundle.module("0.1.0", () -> new RekaModule(manager));
	}

}
