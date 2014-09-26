package reka.pg;

import static reka.util.Util.unchecked;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.jdbc.JdbcModule;

public class PostgresModule extends JdbcModule {

	private static final Logger log = LoggerFactory.getLogger(PostgresModule.class);
	
	static {
		try {
			Class.forName(org.postgresql.Driver.class.getName());
			log.info("loading postgres driver {}", org.postgresql.Driver.class.getName());
		} catch (ClassNotFoundException e) {
			throw unchecked(e);
		}
	}

}
