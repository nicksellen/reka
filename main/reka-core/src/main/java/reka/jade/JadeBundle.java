package reka.jade;

import static reka.api.Path.path;
import reka.core.bundle.RekaBundle;

public class JadeBundle implements RekaBundle {

	@Override
	public void setup(Setup setup) {
		setup.use(path("jade"), () -> new UseJade());
	}

}
