package reka.irc;

import static reka.util.Path.path;
import reka.module.Module;
import reka.module.ModuleDefinition;
import reka.util.Path;

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
