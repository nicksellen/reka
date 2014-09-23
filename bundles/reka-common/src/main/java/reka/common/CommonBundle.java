package reka.common;

import static reka.api.Path.path;
import reka.common.bcrypt.BCryptModule;
import reka.common.markdown.MarkdownBundle;
import reka.core.bundle.BundleConfigurer;

public class CommonBundle implements BundleConfigurer {

	@Override
	public void setup(BundleSetup bundle) {
		bundle.module(path("bcrypt"), "0.1.0", () -> new BCryptModule());
		bundle.bundle(new MarkdownBundle());
	}
	

}
