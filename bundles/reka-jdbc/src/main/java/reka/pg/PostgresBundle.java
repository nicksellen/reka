package reka.pg;

import static reka.api.Path.path;
import reka.core.bundle.RekaBundle;


public class PostgresBundle implements RekaBundle {

	@Override
	public void setup(BundleSetup bundle) {
		bundle.module(path("postgres"), () -> new PostgresModule());
	}

}
