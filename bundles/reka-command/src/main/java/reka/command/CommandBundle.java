package reka.command;

import static reka.api.Path.path;
import reka.core.bundle.RekaBundle;

public class CommandBundle implements RekaBundle {

	@Override
	public void setup(BundleSetup setup) {
		setup.use(path("command"), () -> new CommandModule());
	}

}
