package reka.clojure;

import static reka.util.Path.path;
import reka.module.Module;
import reka.module.ModuleDefinition;
import reka.util.Path;

public class ClojureModule implements Module {

	@Override
	public Path base() {
		return path("clojure");
	}
	
	public void setup(ModuleDefinition module) {
		module.main(() -> new ClojureConfigurer());
	}
	
}
