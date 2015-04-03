package reka.markdown;

import static reka.api.Path.root;
import reka.api.Path;
import reka.module.Module;
import reka.module.ModuleDefinition;

public class MarkdownModule implements Module {

	@Override
	public Path base() {
		return root();
	}
	
	@Override
	public void setup(ModuleDefinition module) {
		module.main(() -> new MarkdownModuleConfigurer());
		module.converter(new MarkdownConverter());
	}

}
