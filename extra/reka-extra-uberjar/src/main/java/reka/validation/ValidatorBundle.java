package reka.validation;

import static reka.api.Path.path;
import reka.core.bundle.RekaBundle;

public class ValidatorBundle implements RekaBundle {

	@Override
	public void setup(BundleSetup setup) {
		setup.use(path("validator"), () -> new UseValidator());
	}

}
