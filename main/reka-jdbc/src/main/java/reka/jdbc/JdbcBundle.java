package reka.jdbc;

import static reka.api.Path.path;
import reka.core.bundle.RekaBundle;
import reka.h2.H2Bundle;
import reka.pg.PostgresBundle;

public class JdbcBundle implements RekaBundle {

	@Override
	public void setup(BundleSetup setup) {
		setup.bundle(new H2Bundle());
		setup.bundle(new PostgresBundle());
		setup.use(path("jdbc"), () -> new UseJdbc());
	}

}
