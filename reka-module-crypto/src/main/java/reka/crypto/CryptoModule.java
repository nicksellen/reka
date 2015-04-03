package reka.crypto;

import static reka.api.Path.root;
import reka.api.Path;
import reka.crypto.bcrypt.BCryptModuleConfigurer;
import reka.module.Module;
import reka.module.ModuleDefinition;

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
