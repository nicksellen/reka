package reka.nashorn;

import static reka.api.Path.path;
import reka.api.Path;
import reka.core.bundle.BundleConfigurer;

// http://docs.oracle.com/javase/8/docs/technotes/guides/scripting/nashorn/intro.html#sthref14

// http://winterbe.com/posts/2014/04/05/java8-nashorn-tutorial/

// threading: https://blogs.oracle.com/nashorn/entry/nashorn_multi_threading_and_mt

public class NashornBundle implements BundleConfigurer {

	@Override
	public Path base() {
		return path("nashorn");
	}

	@Override
	public void setup(BundleSetup bundle) {
		bundle.module("0.1.0", () -> new NashornModule());
	}

}
