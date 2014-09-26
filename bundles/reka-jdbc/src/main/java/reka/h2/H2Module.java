package reka.h2;

import static reka.util.Util.unchecked;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.jdbc.JdbcModule;
import reka.pg.PostgresModule;

public class H2Module extends JdbcModule {
	
	private static final Logger log = LoggerFactory.getLogger(PostgresModule.class);

	static {
		try {
			Class.forName(org.h2.Driver.class.getName());
			log.info("loading postgres driver {}", org.h2.Driver.class.getName());
		} catch (ClassNotFoundException e) {
			throw unchecked(e);
		}
	}

}
