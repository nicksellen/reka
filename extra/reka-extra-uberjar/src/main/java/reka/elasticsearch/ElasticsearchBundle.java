package reka.elasticsearch;

import static reka.api.Path.path;
import reka.core.bundle.RekaBundle;

public class ElasticsearchBundle implements RekaBundle {

	@Override
	public void setup(Setup setup) {
		setup.use(path("elasticsearch"), () -> new UseElasticsearch());
	}

}
