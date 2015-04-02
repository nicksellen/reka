package reka.postgres;

import static java.lang.String.format;
import static reka.config.configurer.Configurer.Preconditions.checkConfig;
import static reka.util.Util.unchecked;

import java.net.URI;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.api.IdentityKey;
import reka.api.flow.Flow;
import reka.config.Config;
import reka.config.configurer.annotations.Conf;
import reka.core.builder.TriggerHelper;
import reka.core.setup.AppSetup;
import reka.core.setup.ModuleSetupContext;
import reka.jdbc.DBCP2ConnectionProvider;
import reka.jdbc.JdbcBaseModule;
import reka.jdbc.JdbcConnectionProvider;

import com.impossibl.postgres.api.jdbc.PGConnection;
import com.impossibl.postgres.jdbc.PGDataSource;


public class PostgresConfigurer extends JdbcBaseModule {

	protected static final IdentityKey<PGDataSource> NOTIFY_DS = IdentityKey.named("notify data source");
	protected static final IdentityKey<PGConnection> NOTIFY_CONNECTION = IdentityKey.named("notify connection"); 
	
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
	
	private final TriggerHelper triggers = new TriggerHelper();
	
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
	
	private final Map<String,IdentityKey<Flow>> triggerKeys = new HashMap<>();
	
	@Conf.Each("on")
	public void on(Config config) {
		checkConfig(config.hasValue(), "must have value");
		String channel = config.valueAsString();
		triggerKeys.computeIfAbsent(channel, ch -> IdentityKey.named(ch));
		triggers.add(triggerKeys.get(channel), config.body());
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
	
	private String asyncJdbcUrl() {
		return jdbcUrl().replaceFirst("^jdbc:postgresql:", "pgsql:");
	}

	@Override
	public void setup(AppSetup app) {
		super.setup(app);
		
		ModuleSetupContext ctx = app.ctx();
		
		if (!triggers.isEmpty()) {
			
			app.onDeploy(init -> {
				init.run("setup notify connection pool", () -> {
					PGDataSource ds = new PGDataSource();
					try {
						URI url = new URI(asyncJdbcUrl());
						ds.setHost(url.getHost());
						ds.setDatabase(url.getPath().replaceFirst("^/", ""));
						ds.setPort(url.getPort());
						ds.setUser(username);
						ds.setPassword(password);
						ctx.put(NOTIFY_DS, ds);
					} catch (Exception e) {
						throw unchecked(e);
					}
				});
			});
			
			app.buildFlows(triggers.build(), reg -> {
				try {
					
					PGDataSource ds = app.ctx().get(NOTIFY_DS);
					PGConnection connection = (PGConnection) ds.getConnection();
					app.ctx().put(NOTIFY_CONNECTION, connection);
					
					Statement statement = connection.createStatement();
					
					triggers.keySet().forEach(key -> {
						reg.lookup(key).ifPresent(flow -> {
							try {
								connection.addNotificationListener("^" + Pattern.quote(key.name()) + "$", new NotifyFlow(flow));
								statement.execute("LISTEN \"" + key.name() + "\"");
							} catch (Exception e) {
								e.printStackTrace();
								throw unchecked(e);
							}
						});
					});
	
					statement.close();
				} catch (Throwable t) {
					t.printStackTrace();
					throw unchecked(t);
				}
			});
			
			app.onUndeploy("close connection", () -> {
				ctx.remove(NOTIFY_CONNECTION).ifPresent(connection -> {
					try {
						connection.close();
					} catch (Throwable t) {
						log.error("error closing connection", t);
					}
				});
			});
			
		}
		
	}
	
	

}
