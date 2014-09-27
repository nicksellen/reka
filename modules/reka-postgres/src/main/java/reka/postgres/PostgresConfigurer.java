package reka.postgres;

import static java.lang.String.format;
import static reka.config.configurer.Configurer.Preconditions.checkConfig;
import static reka.util.Util.unchecked;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.config.configurer.annotations.Conf;
import reka.jdbc.DBCP2ConnectionProvider;
import reka.jdbc.JdbcBaseModule;
import reka.jdbc.JdbcConnectionProvider;

public class PostgresConfigurer extends JdbcBaseModule {

	private static final Logger log = LoggerFactory.getLogger(PostgresConfigurer.class);
	
	static {
		try {
			Class.forName(org.postgresql.Driver.class.getName());
			log.info("loading postgres driver {}", org.postgresql.Driver.class.getName());
		} catch (ClassNotFoundException e) {
			throw unchecked(e);
		}
	}
	
	private String host = "localhost";
	private int port = 5432;
	private String database;
	
	@Conf.At("host")
	public void host(String val) {
		host = val;
	}
	
	@Conf.At("port")
	public void port(int val) {
		port = val;
	}
	
	@Conf.At("database")
	public void database(String val) {
		database = val;
	}
	
	@Override
	public String jdbcUrl() {
		checkConfig(database != null, "must set database");
		return format("jdbc:postgresql://%s:%s/%s", host, port, database);
	}

	@Override
	public JdbcConnectionProvider connectionProvider(String username, String password) {
		return new DBCP2ConnectionProvider(jdbcUrl(), username, password, true);
	}
	
	

}
