package reka.exec;

import static reka.api.Path.path;
import reka.api.Path;
import reka.core.module.Module;
import reka.core.module.ModuleDefinition;

public class ExecModule implements Module {

	@Override
	public Path base() {
		return path("exec");
	}

	@Override
	public void setup(ModuleDefinition module) {
		module.main(() -> new ExecConfigurer());
	}

}
