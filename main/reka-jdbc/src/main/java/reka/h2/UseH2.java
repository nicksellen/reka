package reka.h2;

import static java.lang.String.format;
import static java.lang.String.join;
import static java.util.Arrays.asList;
import static reka.core.config.ConfigUtils.configToData;
import static reka.util.Util.unchecked;

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
import java.util.UUID;

import org.h2.jdbcx.JdbcConnectionPool;

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
import reka.jdbc.JdbcConnectionProvider;
import reka.jdbc.JdbcInsertConfigurer;
import reka.jdbc.JdbcQueryConfigurer;

public class UseH2 extends UseConfigurer {

	private final Map<String,List<Data>> seeds = new HashMap<>();
	
	private String createSQL;
	private String jdbcURL;
	
	@Conf.At("create")
	public void createSQL(Config config) {
		if (config.hasDocument()) {
			createSQL = config.documentContentAsString();
		} else if (config.hasValue()) {
			createSQL = config.valueAsString();
		}
	}

	@Conf.At("url")
	public void jdbcURL(String val) {
		jdbcURL = val;
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
		
		if (jdbcURL == null) {
			jdbcURL = format("jdbc:h2:mem:%s", UUID.randomUUID().toString());
		}

		JdbcConnectionProvider jdbc = new H2JdbcConnectionProvider(JdbcConnectionPool.create(jdbcURL, "sa", "sa"));
		
		Path poolPath = Path.path("stuff").add(fullPath()).add("pool");
		
		init.operation(asList("query", "q", ""), () -> new JdbcQueryConfigurer(jdbc));
		init.operation("insert", () -> new JdbcInsertConfigurer(jdbc));
		
		init.run("create db", (data) -> {
			data.put(poolPath, Contents.nonSerializableContent(jdbc));
			try (Connection connection = jdbc.getConnection()){
	            Statement stmt = connection.createStatement();
	            stmt.execute(createSQL);
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
	
	private static class H2JdbcConnectionProvider implements JdbcConnectionProvider {
        
        private final JdbcConnectionPool pool;
        
        H2JdbcConnectionProvider(JdbcConnectionPool pool) {
        	this.pool = pool;
		}
        
        @Override
        public Connection getConnection() throws SQLException {
            return pool.getConnection();
        }

        @Override
        public void close() throws Exception {
            pool.dispose();
        }
        
    }

}
