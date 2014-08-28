package reka.mustache;

import static reka.api.Path.path;
import reka.core.bundle.RekaBundle;

public class MustacheBundle implements RekaBundle {

	@Override
	public void setup(BundleSetup bundle) {
		bundle.module(path("mustache"), () -> new MustacheModule());
	}

}
