package reka.process;

import static reka.api.Path.path;
import reka.api.Path;
import reka.module.Module;
import reka.module.ModuleDefinition;

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
