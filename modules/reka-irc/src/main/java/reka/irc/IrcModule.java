package reka.irc;

import static reka.api.Path.path;
import reka.api.Path;
import reka.core.module.Module;
import reka.core.module.ModuleDefinition;

public class IrcModule implements Module {

	@Override
	public Path base() {
		return path("irc");
	}

	@Override
	public void setup(ModuleDefinition module) {
		module.main(() -> new IrcConfigurer());
	}

}
