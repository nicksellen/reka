package reka.process;

import static reka.util.Path.path;
import reka.module.Module;
import reka.module.ModuleDefinition;
import reka.util.Path;

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
