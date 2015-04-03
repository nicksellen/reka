package reka.jade;

import static reka.api.Path.root;
import reka.module.setup.AppSetup;
import reka.module.setup.ModuleConfigurer;

public class JadeConfigurer extends ModuleConfigurer {

	@Override
	public void setup(AppSetup init) {
		init.defineOperation(root(), provider -> new JadeRenderConfigurer());
	}

}
