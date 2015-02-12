package reka.jdbc;

import static reka.api.Path.path;
import static reka.api.Path.root;
import static reka.api.content.Contents.booleanValue;
import static reka.api.content.Contents.doubleValue;
import static reka.api.content.Contents.integer;
import static reka.api.content.Contents.longValue;
import static reka.api.content.Contents.nullValue;
import static reka.api.content.Contents.utf8;
import static reka.util.Util.runtime;
import static reka.util.Util.unchecked;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.api.Path;
import reka.api.content.Content;
import reka.api.data.MapMutation;
import reka.api.data.MutableData;
import reka.api.run.Operation;
import reka.core.data.memory.MutableMemoryData;
import reka.core.util.StringWithVars;
import reka.core.util.StringWithVars.Variable;

public class JdbcQuery implements Operation {

	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger("jdbc-query");

	private final JdbcConfiguration config;
	
	private final StringWithVars query;
	private final boolean firstOnly;
	private final String queryWithPlaceholders;
	private final JdbcConnectionProvider provider;
	private final Path resultField;
	
	private volatile Meta meta;
	
	public JdbcQuery(JdbcConfiguration config, JdbcConnectionProvider provider, StringWithVars query, boolean firstOnly, Path resultPath) {
		this.config = config;
		this.query = query;
		this.firstOnly = firstOnly;
		this.queryWithPlaceholders = query.withPlaceholder("?");
		this.provider = provider;
		this.resultField = resultPath;
	}
	
	@Override
	public void call(MutableData data) {
		
		try {
			
			Connection connection = provider.getConnection();
			
			try {
			
				connection.setAutoCommit(false);
	
				boolean updates = false;
				int updateCount = 0;
				
				List<Content> keys = new ArrayList<>();
				
				PreparedStatement statement = connection.prepareStatement(
						queryWithPlaceholders,
						config.returnGeneratedKeys ? Statement.RETURN_GENERATED_KEYS : Statement.NO_GENERATED_KEYS);
				
				for (int i = 0; i < query.vars().size(); i++) {
					Variable v = query.vars().get(i);
					Optional<Content> o = data.getContent(v.path());
					Object value = null;
					if (o.isPresent()) {
						value = o.get().value();
					} else if (v.hasDefaultValue()) {
						value = v.defaultValue();
					}
					statement.setObject(i + 1, value);
				}
				
				if (statement.execute()) { // true -> select, false -> insert/update
					handleResultSet(statement.getResultSet(), data);
				} else {
					updateCount += statement.getUpdateCount();
					updates = true;
					handleKeys(statement.getGeneratedKeys(), keys);
				}
				
				connection.commit();
				
				if (updates) {
					data.put(resultField.add("update-count"), integer(updateCount));
				}
				
				if (!keys.isEmpty()) {
					for (Content key : keys) {
						data.putOrAppend(resultField.add("id"), key);
					}
				}
			
			} catch (Throwable t) {
				connection.rollback();
				throw t;
			} finally {
				provider.finished(connection);
			}
		
		} catch (Throwable t) {	
			t.printStackTrace();
			throw unchecked(t);
		}
	}
	
	private void handleKeys(ResultSet result, Collection<Content> keys) throws SQLException {
		Meta meta = meta(result);
		while (result.next()) {
			keys.add(keyToContent(meta, result, 1));
		}
	}
	

	private Meta meta(ResultSet result) throws SQLException {
		if (meta == null) {
			meta = new Meta(result.getMetaData());
		}
		return meta;
	}
	
	private Content keyToContent(Meta meta, ResultSet result, int column) throws SQLException {
		
		// see http://docs.oracle.com/javase/6/docs/api/constant-values.html#java.sql.Types.STRUCT
		
		switch (meta.types[column]) {
		case Types.LONGNVARCHAR:
		case Types.LONGVARCHAR:
		case Types.CLOB:
		case Types.CHAR:
		case Types.VARCHAR: 
			return utf8(result.getString(column));
		case Types.SMALLINT:
		case Types.INTEGER: 
			return integer(result.getInt(column));
		case Types.BIGINT: 
			return longValue(result.getLong(column));
		case Types.TIMESTAMP: 
			Timestamp ts = result.getTimestamp(column);
			if (ts != null) {
				return longValue(ts.getTime()); // TODO: how to represent dates?	
			} else {
				return nullValue();
			}
		case Types.BOOLEAN:
			return booleanValue(result.getBoolean(column));
		default:
			throw runtime("don't know how to handle column type [%d] / [%s]", meta.types[column], meta.typeNames[column]);
		}
		
	}
	
	private void handleResultSet(ResultSet result, MutableData data) throws SQLException {

		Meta meta = meta(result);
		
		int columnCount = meta.count;
		
		if (firstOnly) {
			
			if (result.next()) {
				data.putMap(root(), map -> {
					for (int column = 1; column < columnCount + 1; column++) {
						putResult(map, meta.keys[column], meta, result, column);
					}
				});
			}
			
		} else {
			
			Path tableName = meta.tablename;
			
			Path p = resultField.isEmpty() ? tableName : resultField;
			
			data.putList(p, list -> {
				while (result.next()) {
					list.addMap(map -> {
						for (int column = 1; column < columnCount + 1; column++) {
							putResult(map, meta.keys[column], meta, result, column);
						}
					});
				}
			});
		
		}
		
	}
	
	private static class Meta {
		
		private final int count;
		private Path tablename;
		private final String[] labels;
		private final Path[] keys;
		private final int[] types;
		private final String[] typeNames;
		
		Meta(ResultSetMetaData meta) throws SQLException {
			try {
				tablename = path(meta.getTableName(1).toLowerCase());
				if (tablename.isEmpty()) {
					tablename = path("results");	
				}
			} catch (SQLException e) {
				tablename = path("results");
			}
			count = meta.getColumnCount();
			labels = new String[count + 1];
			keys = new Path[count + 1];
			types = new int[count + 1];
			typeNames = new String[count + 1];
			for (int i = 1; i < count + 1; i++) {
				labels[i] = meta.getColumnLabel(i).toLowerCase();
				keys[i] = path(labels[i]);
				types[i] = meta.getColumnType(i);
				typeNames[i] = meta.getColumnTypeName(i).toLowerCase();
			}
		}
		
	}
	
	private void putResult(MapMutation item, Path path, Meta meta, ResultSet result, int column) throws IllegalArgumentException, SQLException {

		// see http://docs.oracle.com/javase/6/docs/api/constant-values.html#java.sql.Types.STRUCT
		
		switch (meta.types[column]) {
		case Types.LONGNVARCHAR:
		case Types.LONGVARCHAR:
		case Types.CLOB:
		case Types.CHAR:
		case Types.VARCHAR:
			item.put(path, utf8(result.getString(column)));
			return;
		case Types.SMALLINT:
		case Types.INTEGER:
			item.put(path, integer(result.getInt(column)));
			return;
		case Types.NUMERIC:
			// TODO: not sure how to handle the big decimal scenario
			item.put(path, doubleValue(result.getBigDecimal(column).doubleValue()));
			break;
		case Types.BIGINT:
			item.put(path, longValue(result.getLong(column)));
			return;
		case Types.TIMESTAMP: 
			Timestamp ts = result.getTimestamp(column);
			if (ts != null) {
				item.put(path, longValue(ts.getTime()));
				return; // TODO: how to represent dates?	
			} else {
				item.put(path, nullValue());
				return;
			}
		case Types.BIT:
		case Types.BOOLEAN:
			item.put(path, booleanValue(result.getBoolean(column)));
			return;
		default:
			switch (meta.typeNames[column]) {
			case "uuid":
				item.put(path, utf8(result.getString(column)));
				break;
			case "json":
				String jsonStr = result.getString(column);
				if (jsonStr != null) {
					try {
						@SuppressWarnings("unchecked")
						Map<String,Object> m = json.readValue(jsonStr, Map.class);
						item.put(path, MutableMemoryData.createFromMap(m));
					} catch (IOException e) {
						throw unchecked(e);
					}
				} else {
					item.put(path, nullValue());
				}
				return;
			case "bool":
				item.put(path, booleanValue(result.getBoolean(column)));
				break;
			default:
				throw runtime("don't know how to handle column type [%d] / [%s]", meta.types[column], meta.typeNames[column]);
			}
		}
	}
	
	private static final ObjectMapper json = new ObjectMapper();
}
