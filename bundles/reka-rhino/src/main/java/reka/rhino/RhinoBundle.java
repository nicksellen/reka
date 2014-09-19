package reka.rhino;

import static reka.api.Path.path;
import reka.core.bundle.BundleConfigurer;

public class RhinoBundle implements BundleConfigurer {

	@Override
	public void setup(BundleSetup bundle) {
		bundle.module(path("rhino"), () -> new RhinoModule());
	}

}
