package reka.modules.admin;

import static reka.util.Path.path;
import reka.app.manager.ApplicationManager;
import reka.module.Module;
import reka.module.ModuleDefinition;
import reka.util.Path;

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
