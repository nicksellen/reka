package reka.jade;

import static reka.api.Path.path;
import reka.api.Path;
import reka.core.module.Module;
import reka.core.module.ModuleDefinition;

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
