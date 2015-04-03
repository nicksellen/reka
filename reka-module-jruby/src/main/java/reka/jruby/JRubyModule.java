package reka.jruby;

import static reka.util.Path.path;
import reka.module.Module;
import reka.module.ModuleDefinition;
import reka.util.Path;

public class JRubyModule implements Module {

	@Override
	public Path base() {
		return path("jruby");
	}
	
	@Override
	public void setup(ModuleDefinition module) {
		module.main(() -> new JRubyConfigurer());
	}

}
