package reka.exec;

import static reka.util.Path.path;
import reka.module.Module;
import reka.module.ModuleDefinition;
import reka.util.Path;

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
