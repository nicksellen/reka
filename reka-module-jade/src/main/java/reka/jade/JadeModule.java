package reka.jade;

import static reka.util.Path.path;
import reka.module.Module;
import reka.module.ModuleDefinition;
import reka.util.Path;

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
