package reka.jruby;

import static reka.api.Path.slashes;
import reka.core.bundle.BundleConfigurer;

public class JRubyBundle implements BundleConfigurer {
	
	@Override
	public void setup(BundleSetup bundle) {
		bundle.module(slashes("jruby"), () -> new JRubyModule());
	}

}
