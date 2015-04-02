package reka.crypto;

import static reka.api.Path.root;
import reka.api.Path;
import reka.core.module.Module;
import reka.core.module.ModuleDefinition;
import reka.crypto.bcrypt.BCryptModuleConfigurer;

public class CryptoModule implements Module {

	@Override
	public Path base() {
		return root();
	}
	
	@Override
	public void setup(ModuleDefinition module) {
		module.submodule(Path.path("bcrypt"), () -> new BCryptModuleConfigurer());
	}

}
