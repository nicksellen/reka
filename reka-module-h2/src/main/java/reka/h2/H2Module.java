package reka.h2;

import static reka.api.Path.path;
import reka.api.Path;
import reka.core.module.Module;
import reka.core.module.ModuleDefinition;


public class H2Module implements Module {

	@Override
	public Path base() {
		return path("h2");
	}

	@Override
	public void setup(ModuleDefinition module) {
		module.main(() -> new H2Configurer());
	}

}
