package reka.elasticsearch;

import static reka.api.Path.path;
import reka.core.bundle.RekaBundle;

public class ElasticsearchBundle implements RekaBundle {

	@Override
	public void setup(BundleSetup setup) {
		setup.use(path("elasticsearch"), () -> new ElasticsearchModule());
	}

}
