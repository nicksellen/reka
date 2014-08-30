package reka.jade;

import static reka.api.Path.root;
import reka.core.bundle.ModuleConfigurer;
import reka.core.bundle.ModuleSetup;

public class JadeModule extends ModuleConfigurer {

	@Override
	public void setup(ModuleSetup init) {
		init.operation(root(), () -> new JadeConfigurer());
	}

}
