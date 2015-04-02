package reka.h2;

import static java.lang.String.format;
import static reka.util.Util.unchecked;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.config.configurer.annotations.Conf;
import reka.jdbc.DBCP2ConnectionProvider;
import reka.jdbc.JdbcBaseModule;
import reka.jdbc.JdbcConnectionProvider;

public class H2Configurer extends JdbcBaseModule {

	private static final Logger log = LoggerFactory.getLogger(H2Configurer.class);

	static {
		try {
			Class.forName(org.h2.Driver.class.getName());
			log.info("loading postgres driver {}", org.h2.Driver.class.getName());
		} catch (ClassNotFoundException e) {
			throw unchecked(e);
		}
	}

	private boolean persist = false;
	private String dbName = UUID.randomUUID().toString();

	@Conf.At("database")
	public void database(String val) {
		dbName = val;
	}

	@Conf.At("persist")
	public void persist(boolean val) {
		persist = val;
	}

	@Override
	public String jdbcUrl() {
		if (persist) {
			String dbPath = dirs().data().resolve(dbName.startsWith("/") ? dbName.substring(1) : dbName).toAbsolutePath().toString();
			return format("jdbc:h2:file:%s", dbPath);
		} else {
			return format("jdbc:h2:mem:%s", dbName);
		}
	}

	@Override
	public JdbcConnectionProvider connectionProvider(String username, String password) {
		return new DBCP2ConnectionProvider(jdbcUrl(), username, password, false);
	}

}
