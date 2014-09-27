package reka.rhino;

import static reka.api.Path.path;
import reka.api.Path;
import reka.core.module.Module;
import reka.core.module.ModuleDefinition;

public class RhinoModule implements Module {

	@Override
	public Path base() {
		return path("rhino");
	}

	@Override
	public void setup(ModuleDefinition module) {
		module.main(() -> new RhinoConfigurer());
	}

}
