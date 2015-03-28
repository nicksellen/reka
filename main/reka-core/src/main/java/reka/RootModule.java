package reka;

import java.util.List;

import reka.core.module.ModuleInfo;
import reka.core.setup.ModuleConfigurer;
import reka.core.setup.AppSetup;
import reka.dirs.AppDirs;

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
