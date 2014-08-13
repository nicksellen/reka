package reka.leveldb;

import static reka.api.Path.path;
import reka.core.bundle.RekaBundle;

public class LevelDBBundle implements RekaBundle {

	@Override
	public void setup(BundleSetup setup) {
		setup.use(path("leveldb"), () -> new LevelDBModule());
	}

}
