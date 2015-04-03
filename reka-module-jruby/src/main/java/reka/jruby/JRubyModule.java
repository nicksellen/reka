package reka.jruby;

import static reka.api.Path.path;
import reka.api.Path;
import reka.module.Module;
import reka.module.ModuleDefinition;

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
