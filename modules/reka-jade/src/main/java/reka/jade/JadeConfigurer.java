package reka.jade;

import static reka.api.Path.root;
import reka.core.setup.ModuleConfigurer;
import reka.core.setup.ModuleSetup;

public class JadeConfigurer extends ModuleConfigurer {

	@Override
	public void setup(ModuleSetup init) {
		init.operation(root(), provider -> new JadeRenderConfigurer());
	}

}
