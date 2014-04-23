package reka.javascript;

import static reka.api.Path.path;
import reka.core.bundle.RekaBundle;

public class JavascriptBundle implements RekaBundle {

	@Override
	public void setup(Setup setup) {
		setup.use(path("javascript"), () -> new UseJavascript());
	}

}
