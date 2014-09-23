package reka.jade;

import static reka.api.Path.path;
import reka.core.bundle.BundleConfigurer;

public class JadeBundle implements BundleConfigurer {

	@Override
	public void setup(BundleSetup bundle) {
		bundle.module(path("jade"), "0.1.0", () -> new JadeModule());
	}

}
