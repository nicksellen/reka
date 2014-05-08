package reka.pg;

import static reka.api.Path.path;
import reka.core.bundle.RekaBundle;


public class PostgresBundle implements RekaBundle {

	@Override
	public void setup(Setup setup) {
		setup.use(path("postgres"), () -> new UsePostgres());
	}

}
