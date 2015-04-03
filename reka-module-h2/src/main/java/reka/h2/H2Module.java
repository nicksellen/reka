package reka.h2;

import static reka.util.Path.path;
import reka.module.Module;
import reka.module.ModuleDefinition;
import reka.util.Path;


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
