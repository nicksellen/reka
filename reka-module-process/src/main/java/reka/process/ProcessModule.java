package reka.process;

import static reka.api.Path.path;
import reka.api.Path;
import reka.core.module.Module;
import reka.core.module.ModuleDefinition;

public class ProcessModule implements Module {

	@Override
	public Path base() {
		return path("process");
	}

	@Override
	public void setup(ModuleDefinition module) {
		module.main(() -> new ProcessConfigurer());
	}

}
