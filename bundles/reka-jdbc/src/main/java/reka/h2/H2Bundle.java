package reka.h2;

import static reka.api.Path.path;
import reka.core.bundle.BundleConfigurer;


public class H2Bundle implements BundleConfigurer {

	@Override
	public void setup(BundleSetup bundle) {
		bundle.module(path("h2"), () -> new H2Module());
	}

}
