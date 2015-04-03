package reka.module;

import reka.util.Path;

public interface Module {
	
	Path base();
	void setup(ModuleDefinition module);

}
