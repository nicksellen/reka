package reka.jdbc;

import static java.lang.String.format;
import static java.lang.String.join;
import static reka.api.Path.path;
import static reka.api.Path.root;
import static reka.config.configurer.Configurer.Preconditions.checkConfig;
import static reka.core.config.ConfigUtils.configToData;
import static reka.util.Util.unchecked;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.api.IdentityKey;
import reka.api.content.Content;
import reka.api.data.Data;
import reka.config.Config;
import reka.config.configurer.annotations.Conf;
import reka.core.setup.ModuleConfigurer;
import reka.core.setup.ModuleSetup;
import reka.core.util.StringWithVars;
import reka.core.util.StringWithVars.Variable;

public abstract class JdbcBaseModule extends ModuleConfigurer {
	
	protected static final IdentityKey<JdbcConnectionProvider> POOL = IdentityKey.named("connection pool");

	@SuppressWarnings("unused")
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	private final Map<String,List<Data>> seeds = new HashMap<>();
	
	private List<String> sqls = new ArrayList<>();
	private String username, password;
	
	private boolean returnGeneratedKeys = false;
	
	private final Map<String,String> migrations = new HashMap<>();
	private int migrationNum = 1;
	
	@Conf.Each("run")
	public void run(Config config) {
		if (config.hasDocument()) {
			sqls.add(config.documentContentAsString());
		} else if (config.hasValue()) {
			sqls.add(config.valueAsString());
		}
	}
	
	@Conf.Each("migration")
	public void migration(Config config) {
		checkConfig(config.hasValue() && config.hasDocument(), "must include value and document");
		migrations.put(format("%s %s", migrationNum++, config.valueAsString()), config.documentContentAsString());
	}
	
	@Conf.At("returned-generated-keys")
	public void returnGeneratedKeys(Boolean val) {
		returnGeneratedKeys = val;
	}

	public abstract String jdbcUrl();
	public abstract JdbcConnectionProvider connectionProvider(String username, String password); 
	
	@Conf.At("username")
	public void username(String val) {
		username = val;
	}
	
	@Conf.At("password")
	public void password(String val) {
		password = val;
	}	
	
	@Conf.Each("seed")
	public void seed(Config config) {
		if (!config.hasBody()) return;
		
		String table = config.valueAsString();
		seeds.putIfAbsent(table, new ArrayList<>());
		
		for (Config entry : config.body().each()) {
			if (!entry.hasBody()) return;
			seeds.get(table).add(configToData(entry.body()));
		}
		
	}

	@Override
	public void setup(ModuleSetup module) {
		
		JdbcConfiguration config = new JdbcConfiguration(returnGeneratedKeys);
		
		String url = jdbcUrl();

		if (username == null) username = "sa";
		if (password == null) password = "sa";
		
		module.operation(root(), provider -> new JdbcQueryConfigurer(config));
		module.operation(path("insert"), provider -> new JdbcInsertConfigurer());
		
		module.setupInitializer(init -> {
			
			init.run("create connection pool", store -> {
				store.put(POOL, connectionProvider(username, password));
			});

			if (!migrations.isEmpty()) {
				init.run("run migrations", store -> {
					Path tmpdir = null;
					try {
						tmpdir = Files.createTempDirectory("jdbc");
					
						for (Entry<String, String> e : migrations.entrySet()) {
							Pattern VERSION_BIT = Pattern.compile("^[\\._0-9]+");
							String name = e.getKey();
							Matcher m = VERSION_BIT.matcher(name);
							m.find();
							String num = m.group();
							String rest = name.substring(m.end());
							rest = rest.replaceFirst(" ", "__").replaceAll(" ", "_");
							java.nio.file.Path tmp = tmpdir.resolve(format("V%s%s.sql", num, rest));
							try {
								Files.write(tmp, e.getValue().getBytes(StandardCharsets.UTF_8));
							} catch (Exception e2) {
								throw unchecked(e2);
							}
						}
						Flyway flyway = new Flyway();
						flyway.setClassLoader(Flyway.class.getClassLoader());
						flyway.setDataSource(store.get(POOL).dataSource());
						flyway.setLocations(format("filesystem:%s", tmpdir.toFile().getAbsolutePath()));
						flyway.migrate();
						
					} catch (IOException e) {
						throw unchecked(e);
					} finally {
						if (tmpdir != null) {
							try { 
								FileUtils.deleteDirectory(tmpdir.toFile());
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					}
				});
			}
			
			init.run("run sql", store -> {
				try (Connection connection = store.get(POOL).getConnection()){
		            Statement stmt = connection.createStatement();
		            for (String sql : sqls) {
		            	stmt.execute(sql);
		            }
		        } catch (SQLException e) {
		        	throw unchecked(e);
		        }
			});
			
			init.run("seed data", store -> {
				
				try (Connection conn = store.get(POOL).getConnection()) {
					for (Entry<String, List<Data>> e : seeds.entrySet()) {
						String table = e.getKey();
						List<Data> entries = e.getValue();
						if (entries.isEmpty()) continue;
						
						for (Data entry : entries) {
							
							StringBuilder sb = new StringBuilder();
							
							List<String> fields = new ArrayList<>();
							List<String> valuePlaceholders = new ArrayList<>();
							
							entry.forEachContent((path, content) -> {
								String fieldname = path.dots(); 
								fields.add(fieldname);
								valuePlaceholders.add(format(":{%s}", fieldname));
							});
							
							sb.append("insert into ").append(table)
								.append("(").append(join(",", fields)).append(")")
								.append("values")
									.append("(").append(join(", ", valuePlaceholders)).append(")");
						
							StringWithVars query = StringWithVars.compile(sb.toString());
							
							PreparedStatement statement = conn.prepareStatement(query.withPlaceholder("?"));
							
							for (int i = 0; i < query.vars().size(); i++) {
								Variable v = query.vars().get(i);
								Optional<Content> content = entry.getContent(v.path());
								Object value = null;
								if (content.isPresent()) {
									value = content.get().value();
								} else if (v.defaultValue() != null) {
									value = v.defaultValue();
								}
								statement.setObject(i + 1, value);
							}
							
							statement.execute();
						
						}
					}
				} catch (SQLException e) {
					throw unchecked(e);
				}
			});
		
		});
		
		module.status(store -> new JdbcStatusProvider(url, store.get(POOL)));
		
		module.onShutdown("close connection pool", store -> {
			store.lookup(POOL).ifPresent(jdbc -> { 
				try {
					jdbc.close();
				} catch (Exception e) {
					e.printStackTrace(); // whatever
				}
			});
		});
		
	}

}
