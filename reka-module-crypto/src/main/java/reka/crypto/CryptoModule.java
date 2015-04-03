package reka.crypto;

import static reka.util.Path.root;
import reka.crypto.bcrypt.BCryptModuleConfigurer;
import reka.module.Module;
import reka.module.ModuleDefinition;
import reka.util.Path;

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
