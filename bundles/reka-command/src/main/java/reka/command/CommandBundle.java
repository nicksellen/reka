package reka.command;

import static reka.api.Path.path;
import reka.core.bundle.BundleConfigurer;

public class CommandBundle implements BundleConfigurer {

	@Override
	public void setup(BundleSetup bundle) {
		bundle.module(path("command"), "0.1.0", () -> new CommandModule());
	}

}
