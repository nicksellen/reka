package reka.irc;

import static reka.api.Path.path;
import reka.api.Path;
import reka.module.Module;
import reka.module.ModuleDefinition;

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
