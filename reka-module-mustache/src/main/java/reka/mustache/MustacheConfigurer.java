package reka.mustache;

import static reka.api.Path.root;
import reka.core.setup.ModuleConfigurer;
import reka.core.setup.AppSetup;

public class MustacheConfigurer extends ModuleConfigurer {

	@Override
	public void setup(AppSetup init) {
		init.defineOperation(root(), provider -> new MustacheRenderConfigurer());
	}

}
