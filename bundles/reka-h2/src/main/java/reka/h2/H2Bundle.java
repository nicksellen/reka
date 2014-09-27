package reka.h2;

import static reka.api.Path.path;
import reka.api.Path;
import reka.core.bundle.BundleConfigurer;


public class H2Bundle implements BundleConfigurer {

	@Override
	public Path base() {
		return path("h2");
	}

	@Override
	public void setup(BundleSetup bundle) {
		bundle.module("0.1.0", () -> new H2Module());
	}

}
