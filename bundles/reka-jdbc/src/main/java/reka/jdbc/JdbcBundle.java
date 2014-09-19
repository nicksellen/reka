package reka.jdbc;

import static reka.api.Path.path;
import reka.core.bundle.BundleConfigurer;
import reka.h2.H2Bundle;
import reka.pg.PostgresBundle;

public class JdbcBundle implements BundleConfigurer {

	@Override
	public void setup(BundleSetup bundle) {
		bundle.bundle(new H2Bundle());
		bundle.bundle(new PostgresBundle());
		bundle.module(path("jdbc"), () -> new JdbcModule());
	}

}
