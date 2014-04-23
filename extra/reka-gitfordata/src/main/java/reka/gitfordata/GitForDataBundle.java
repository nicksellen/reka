package reka.gitfordata;

import static reka.api.Path.path;
import reka.core.bundle.RekaBundle;

public class GitForDataBundle implements RekaBundle {

	@Override
	public void setup(Setup setup) {
		setup.use(path("store"), () -> new UseGitForData());
	}

}
