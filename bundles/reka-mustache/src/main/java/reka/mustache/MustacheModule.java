package reka.mustache;

import static reka.api.Path.root;
import reka.core.bundle.ModuleConfigurer;
import reka.core.setup.ModuleSetup;

public class MustacheModule extends ModuleConfigurer {

	@Override
	public void setup(ModuleSetup init) {
		init.operation(root(), provider -> new MustacheConfigurer());
	}

}
