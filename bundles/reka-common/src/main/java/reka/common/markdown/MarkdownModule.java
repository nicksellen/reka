package reka.common.markdown;

import static reka.api.Path.root;
import reka.core.setup.ModuleConfigurer;
import reka.core.setup.ModuleSetup;

public class MarkdownModule extends ModuleConfigurer {

	@Override
	public void setup(ModuleSetup module) {
		module.operation(root(), provider -> new MarkdownConfigurer());
	}

}
