package reka.jade;

import static reka.api.Path.path;
import reka.api.Path;
import reka.core.bundle.BundleConfigurer;

public class JadeBundle implements BundleConfigurer {

	@Override
	public Path base() {
		return path("jade");
	}

	@Override
	public void setup(BundleSetup bundle) {
		bundle.module("0.1.0", () -> new JadeModule());
	}

}
