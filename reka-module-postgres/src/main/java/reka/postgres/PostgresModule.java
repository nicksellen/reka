package reka.postgres;

import static reka.util.Path.path;
import reka.module.Module;
import reka.module.ModuleDefinition;
import reka.util.Path;


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
