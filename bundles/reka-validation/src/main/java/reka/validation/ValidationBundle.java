package reka.validation;

import static reka.api.Path.path;
import reka.core.bundle.RekaBundle;

public class ValidationBundle implements RekaBundle {

	@Override
	public void setup(BundleSetup setup) {
		setup.use(path("validator"), () -> new ValidatorModule());
	}

}
