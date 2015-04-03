package reka.clojure;

import static reka.api.Path.path;
import reka.api.Path;
import reka.module.Module;
import reka.module.ModuleDefinition;

public class ClojureModule implements Module {

	@Override
	public Path base() {
		return path("clojure");
	}
	
	public void setup(ModuleDefinition module) {
		module.main(() -> new ClojureConfigurer());
	}
	
}
