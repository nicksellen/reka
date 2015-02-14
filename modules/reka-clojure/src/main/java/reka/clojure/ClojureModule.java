package reka.clojure;

import static reka.api.Path.path;
import reka.api.Path;
import reka.core.module.Module;
import reka.core.module.ModuleDefinition;

public class ClojureModule implements Module {

	@Override
	public Path base() {
		return path("clojure");
	}
	
	public void setup(ModuleDefinition module) {
		module.main(() -> new ClojureConfigurer());
	}
	
}
