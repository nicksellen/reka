package reka.jruby;

import static reka.api.Path.slashes;
import reka.core.bundle.RekaBundle;

public class JRubyBundle implements RekaBundle {
	
	@Override
	public void setup(BundleSetup bundle) {
		bundle.module(slashes("jruby"), () -> new JRubyModule());
	}

}
