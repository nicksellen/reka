package reka.rhino;

import static reka.api.Path.path;
import reka.core.bundle.RekaBundle;

public class RhinoBundle implements RekaBundle {

	@Override
	public void setup(BundleSetup setup) {
		setup.use(path("rhino"), () -> new RhinoModule());
	}

}
