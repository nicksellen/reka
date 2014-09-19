package reka.jade;

import static reka.api.Path.path;
import reka.core.bundle.BundleConfigurer;

public class JadeBundle implements BundleConfigurer {

	@Override
	public void setup(BundleSetup bundle) {
		bundle.module(path("jade"), () -> new JadeModule());
	}

}
