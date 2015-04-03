package reka.module;

import reka.api.Path;

public interface Module {
	
	Path base();
	void setup(ModuleDefinition module);

}
