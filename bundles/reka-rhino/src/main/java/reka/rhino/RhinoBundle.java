package reka.rhino;

import static reka.api.Path.path;
import reka.api.Path;
import reka.core.bundle.BundleConfigurer;

public class RhinoBundle implements BundleConfigurer {

	@Override
	public Path base() {
		return path("rhino");
	}

	@Override
	public void setup(BundleSetup bundle) {
		bundle.module("0.1.0", () -> new RhinoModule());
	}

}
