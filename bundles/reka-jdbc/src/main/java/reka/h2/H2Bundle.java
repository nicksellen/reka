package reka.h2;

import static reka.api.Path.path;
import reka.core.bundle.RekaBundle;


public class H2Bundle implements RekaBundle {

	@Override
	public void setup(BundleSetup setup) {
		setup.use(path("h2"), () -> new H2Module());
	}

}
