package reka.process;

import static reka.api.Path.path;
import reka.core.bundle.RekaBundle;

public class ProcessBundle implements RekaBundle {

	@Override
	public void setup(BundleSetup bundle) {
		bundle.module(path("process"), () -> new ProcessModule());
	}

}
