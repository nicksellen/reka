package reka.mustache;

import static reka.api.Path.path;
import reka.api.Path;
import reka.module.Module;
import reka.module.ModuleDefinition;

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
