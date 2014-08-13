package reka;

import reka.core.bundle.ModuleConfigurer;
import reka.core.bundle.ModuleInit;

public class RootModule extends ModuleConfigurer {
	
	public RootModule() {
		type("root").isRoot(true);
	}

	@Override
	public void setup(ModuleInit register) { }

}
