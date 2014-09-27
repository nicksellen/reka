package reka.common;

import static reka.api.Path.path;
import static reka.api.Path.root;
import reka.api.Path;
import reka.common.bcrypt.BCryptModule;
import reka.common.markdown.MarkdownConverter;
import reka.common.markdown.MarkdownModule;
import reka.core.module.Module;
import reka.core.module.ModuleDefinition;

public class CommonModule implements Module {

	@Override
	public Path base() {
		return root();
	}
	
	@Override
	public void setup(ModuleDefinition module) {
		module.submodule(path("bcrypt"), () -> new BCryptModule());
		module.submodule(path("markdown"), () -> new MarkdownModule());
		module.converter(new MarkdownConverter());
	}

}
