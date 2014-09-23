package reka.h2;

import static reka.api.Path.path;
import reka.core.bundle.BundleConfigurer;


public class H2Bundle implements BundleConfigurer {

	@Override
	public void setup(BundleSetup bundle) {
		bundle.module(path("h2"), "0.1.0", () -> new H2Module());
	}

}
