package reka.jdbc;

import static java.lang.String.format;
import static java.lang.String.join;
import static reka.util.Util.unchecked;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import reka.api.content.Content;
import reka.api.data.Data;
import reka.api.data.MutableData;
import reka.api.run.Operation;
import reka.core.util.StringWithVars;
import reka.core.util.StringWithVars.Variable;

public class JdbcInsert implements Operation {
	
	private final JdbcConnectionProvider jdbc;
	private final String table;
	private final List<Data> values;
	
	public JdbcInsert(JdbcConnectionProvider jdbc, String table, List<Data> values) {
		this.jdbc = jdbc;
		this.table = table;
		this.values = new ArrayList<>(values);
	}

	@Override
	public void call(MutableData data) {
		try (Connection conn = jdbc.getConnection()) {
			for (Data entry : values) {
				
				StringBuilder sb = new StringBuilder();
				
				List<String> fields = new ArrayList<>();
				List<String> valuePlaceholders = new ArrayList<>();
				
				entry.forEachContent((path, content) -> {
					String fieldname = path.dots(); 
					fields.add(fieldname);
					valuePlaceholders.add(format("{%s}", fieldname));
				});
				
				// TODO: make this safer... (table name cannot be a prepared query param though)
				sb.append("insert into ").append(table)
					.append("(").append(join(",", fields)).append(")")
					.append("values")
						.append("(").append(join(", ", valuePlaceholders)).append(")");
			
				StringWithVars query = StringWithVars.compile(sb.toString());
				
				PreparedStatement statement = conn.prepareStatement(query.withPlaceholder("?"));
				
				for (int i = 0; i < query.vars().size(); i++) {
					Variable v = query.vars().get(i);
					Optional<Content> o = entry.getContent(v.path());
					Object value = null;
					if (o.isPresent()) {
						value = o.get().value();
					} else if (v.defaultValue() != null) {
						value = v.defaultValue();
					}
					statement.setObject(i + 1, value);
				}
				
				statement.execute();
			}
		} catch (SQLException e) {
			throw unchecked(e);
		}
	}

}
