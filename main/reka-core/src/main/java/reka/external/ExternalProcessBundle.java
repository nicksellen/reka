package reka.external;

import static reka.api.Path.path;
import reka.core.bundle.RekaBundle;

public class ExternalProcessBundle implements RekaBundle {

	@Override
	public void setup(BundleSetup setup) {
		setup.use(path("external"), () -> new UseExternal());
	}

}
