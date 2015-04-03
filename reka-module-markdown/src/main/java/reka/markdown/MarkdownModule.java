package reka.markdown;

import static reka.util.Path.root;
import reka.module.Module;
import reka.module.ModuleDefinition;
import reka.util.Path;

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
