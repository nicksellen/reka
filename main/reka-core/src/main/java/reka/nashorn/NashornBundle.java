package reka.nashorn;

import static reka.api.Path.path;
import reka.core.bundle.RekaBundle;

// http://docs.oracle.com/javase/8/docs/technotes/guides/scripting/nashorn/intro.html#sthref14

// http://winterbe.com/posts/2014/04/05/java8-nashorn-tutorial/

// threading: https://blogs.oracle.com/nashorn/entry/nashorn_multi_threading_and_mt

public class NashornBundle implements RekaBundle {

	@Override
	public void setup(Setup setup) {
		setup.use(path("nashorn"), () -> new UseNashorn());
	}

}
