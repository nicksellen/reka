package reka.core.module;

import reka.api.Path;

public interface Module {
	
	Path base();
	void setup(ModuleDefinition module);

}
