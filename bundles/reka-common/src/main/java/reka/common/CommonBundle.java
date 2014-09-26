package reka.common;

import static reka.api.Path.path;
import static reka.api.Path.root;
import reka.api.Path;
import reka.common.bcrypt.BCryptModule;
import reka.common.markdown.MarkdownConverter;
import reka.common.markdown.MarkdownModule;
import reka.core.bundle.BundleConfigurer;

public class CommonBundle implements BundleConfigurer {

	@Override
	public Path base() {
		return root();
	}
	
	@Override
	public void setup(BundleSetup bundle) {
		bundle.submodule(path("bcrypt"), "0.1.0", () -> new BCryptModule());
		bundle.submodule(path("markdown"), "0.1.0", () -> new MarkdownModule());
		bundle.converter(new MarkdownConverter());
	}

}
