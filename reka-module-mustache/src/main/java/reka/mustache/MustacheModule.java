package reka.mustache;

import static reka.util.Path.path;
import reka.module.Module;
import reka.module.ModuleDefinition;
import reka.util.Path;

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
