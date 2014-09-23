package reka.pg;

import static reka.api.Path.path;
import reka.core.bundle.BundleConfigurer;


public class PostgresBundle implements BundleConfigurer {

	@Override
	public void setup(BundleSetup bundle) {
		bundle.module(path("postgres"), "0.1.0", () -> new PostgresModule());
	}

}
