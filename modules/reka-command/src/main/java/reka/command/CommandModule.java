package reka.command;

import static reka.api.Path.path;
import reka.api.Path;
import reka.core.module.Module;
import reka.core.module.ModuleDefinition;

public class CommandModule implements Module {

	@Override
	public Path base() {
		return path("command");
	}
	
	@Override
	public void setup(ModuleDefinition module) {
		module.main(() -> new CommandConfigurer());
	}

}
