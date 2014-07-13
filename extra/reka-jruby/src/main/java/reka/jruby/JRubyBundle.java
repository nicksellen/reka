package reka.jruby;

import static reka.api.Path.slashes;
import reka.core.bundle.RekaBundle;

public class JRubyBundle implements RekaBundle {
	
	/*
	 * need to add bundler/gems into the mix
	 * 	useful: http://yokolet.blogspot.de/2010/10/gems-in-jar-with-redbridge.html
	 * 
	 */

	@Override
	public void setup(BundleSetup setup) {
		setup.use(slashes("jruby"), () -> new UseJRuby());
	}

}
