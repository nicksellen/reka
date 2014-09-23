package reka.process;

import static reka.api.Path.path;
import reka.core.bundle.BundleConfigurer;

public class ProcessBundle implements BundleConfigurer {

	@Override
	public void setup(BundleSetup bundle) {
		bundle.module(path("process"), "0.1.0", () -> new ProcessModule());
	}

}
