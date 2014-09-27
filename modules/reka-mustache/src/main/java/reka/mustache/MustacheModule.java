package reka.mustache;

import static reka.api.Path.path;
import reka.api.Path;
import reka.core.module.Module;
import reka.core.module.ModuleDefinition;

public class MustacheModule implements Module {

	@Override
	public Path base() {
		return path("mustache");
	}

	@Override
	public void setup(ModuleDefinition module) {
		module.main(() -> new MustacheConfigurer());
	}

}
