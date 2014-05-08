package reka.jdbc;

import static java.lang.String.format;
import static java.lang.String.join;
import static java.util.Arrays.asList;
import static reka.configurer.Configurer.Preconditions.checkConfig;
import static reka.core.config.ConfigUtils.configToData;
import static reka.util.Util.unchecked;

import java.io.File;
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
import java.util.Scanner;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.api.Path;
import reka.api.content.Content;
import reka.api.content.Contents;
import reka.api.data.Data;
import reka.config.Config;
import reka.configurer.annotations.Conf;
import reka.core.bundle.UseConfigurer;
import reka.core.bundle.UseInit;
import reka.core.util.StringWithVars;
import reka.core.util.StringWithVars.Variable;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

public class UseJdbc extends UseConfigurer {

	private final Logger log = LoggerFactory.getLogger(getClass());
	
	private final Map<String,List<Data>> seeds = new HashMap<>();
	
	private List<String> sqls = new ArrayList<>();
	private String url, username, password;
	
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

	@Conf.At("url")
	public void jdbcURL(String val) {
		url = val;
	}
	
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
	public void setup(UseInit init) {
		
		JdbcConfiguration config = new JdbcConfiguration(returnGeneratedKeys);
		
		if (url == null) {
			url = format("jdbc:h2:mem:%s", UUID.randomUUID().toString());
			if (username == null) username = "sa";
			if (password == null) password = "sa";
		}

		JdbcConnectionProvider jdbc = new DBCPConnectionProvider(url, username, password);
		
		Path poolPath = Path.path("stuff").add(fullPath()).add("pool");
		
		init.operation(asList("query", "q", ""), () -> new JdbcQueryConfigurer(config, jdbc));
		init.operation("insert", () -> new JdbcInsertConfigurer(jdbc));

		if (!migrations.isEmpty()) {
		
			init.run("run migrations", (data) -> {
				
				File tmpdir = Files.createTempDir();
				
				try {
				
					for (Entry<String, String> e : migrations.entrySet()) {
						Pattern VERSION_BIT = Pattern.compile("^[\\._0-9]+");
						String name = e.getKey();
						Matcher m = VERSION_BIT.matcher(name);
						m.find();
						String num = m.group();
						String rest = name.substring(m.end());
						rest = rest.replaceFirst(" ", "__").replaceAll(" ", "_");
						java.nio.file.Path tmp = tmpdir.toPath().resolve(format("V%s%s.sql", num, rest));
						try {
							Files.write(e.getValue(), tmp.toFile(), Charsets.UTF_8);
						} catch (Exception e2) {
							throw unchecked(e2);
						}
					}
					
					Flyway flyway = new Flyway();
					flyway.setDataSource(jdbc.dataSource());
					flyway.setLocations(format("filesystem:%s", tmpdir.getAbsolutePath()));
					flyway.migrate();
				
				} finally {
					try {
						FileUtils.deleteDirectory(tmpdir);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				
				return data;
			});
		
		}
		
		init.run("run sql", (data) -> {
			data.put(poolPath, Contents.nonSerializableContent(jdbc));
			try (Connection connection = jdbc.getConnection()){
	            Statement stmt = connection.createStatement();
	            for (String sql : sqls) {
	            	stmt.execute(sql);
	            }
	        } catch (SQLException e) {
	        	throw unchecked(e);
	        }
			return data;
		});
		
		init.run("seed data", (data) -> {
			
			try (Connection conn = jdbc.getConnection()) {
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
			
			return data;
		});
		
	}

}
