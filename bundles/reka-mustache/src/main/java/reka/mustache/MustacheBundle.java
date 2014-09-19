package reka.mustache;

import static reka.api.Path.path;
import reka.core.bundle.BundleConfigurer;

public class MustacheBundle implements BundleConfigurer {

	@Override
	public void setup(BundleSetup bundle) {
		bundle.module(path("mustache"), () -> new MustacheModule());
	}

}
