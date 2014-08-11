package reka.clojure;

import static reka.api.Path.path;
import reka.core.bundle.RekaBundle;

public class ClojureBundle implements RekaBundle {

	public void setup(BundleSetup setup) {
		setup.use(path("clojure"), () -> new UseClojure());
	}
}
