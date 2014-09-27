package reka.admin;

import static reka.api.Path.path;
import reka.ApplicationManager;
import reka.api.Path;
import reka.core.module.Module;
import reka.core.module.ModuleDefinition;

public class AdminModule implements Module {

	@Override
	public Path base() {
		return path("reka");
	}
	
	private final ApplicationManager manager;
	
	public AdminModule(ApplicationManager manager) {
		this.manager = manager;
	}

	@Override
	public void setup(ModuleDefinition module) {
		module.main(() -> new AdminConfigurer(manager));
	}

}
