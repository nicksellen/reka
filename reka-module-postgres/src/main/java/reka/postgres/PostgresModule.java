package reka.postgres;

import static reka.api.Path.path;
import reka.api.Path;
import reka.module.Module;
import reka.module.ModuleDefinition;


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
