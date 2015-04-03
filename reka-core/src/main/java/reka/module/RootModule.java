package reka.module;

import java.util.List;

import reka.module.setup.AppSetup;
import reka.module.setup.ModuleConfigurer;
import reka.util.dirs.AppDirs;

public class RootModule extends ModuleConfigurer {
	
	public RootModule(AppDirs dirs, List<ModuleInfo> modules) {
		name("root");
		isRoot(true);
		dirs(dirs);
		modules(modules);
	}

	@Override
	public void setup(AppSetup module) { }

}
