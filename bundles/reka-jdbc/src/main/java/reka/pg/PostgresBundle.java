package reka.pg;

import static reka.api.Path.path;
import reka.api.Path;
import reka.core.bundle.BundleConfigurer;


public class PostgresBundle implements BundleConfigurer {

	@Override
	public Path base() {
		return path("postgres");
	}

	@Override
	public void setup(BundleSetup bundle) {
		bundle.module("0.1.0", () -> new PostgresModule());
	}

}
