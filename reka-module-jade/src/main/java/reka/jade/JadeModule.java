package reka.jade;

import static reka.api.Path.path;
import reka.api.Path;
import reka.module.Module;
import reka.module.ModuleDefinition;

public class JadeModule implements Module {

	@Override
	public Path base() {
		return path("jade");
	}

	@Override
	public void setup(ModuleDefinition module) {
		module.main(() -> new JadeConfigurer());
	}

}
