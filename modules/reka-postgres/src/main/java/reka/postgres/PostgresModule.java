package reka.postgres;

import static reka.api.Path.path;
import reka.api.Path;
import reka.core.module.Module;
import reka.core.module.ModuleDefinition;


public class PostgresModule implements Module {

	@Override
	public Path base() {
		return path("postgres");
	}

	@Override
	public void setup(ModuleDefinition module) {
		module.main(() -> new PostgresConfigurer());
	}

}
