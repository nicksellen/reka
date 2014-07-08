package reka.jdbc;

import static reka.api.Path.path;
import static reka.api.Path.PathElements.index;
import static reka.api.content.Contents.falseValue;
import static reka.api.content.Contents.integer;
import static reka.api.content.Contents.longValue;
import static reka.api.content.Contents.nullValue;
import static reka.api.content.Contents.trueValue;
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
import java.util.concurrent.Executors;

import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.api.Path;
import reka.api.content.Content;
import reka.api.data.MutableData;
import reka.api.run.SyncOperation;
import reka.core.data.memory.MutableMemoryData;
import reka.core.util.StringWithVars;
import reka.core.util.StringWithVars.Variable;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

public class JdbcQuery implements SyncOperation {

	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger("jdbc-query");
	
	@SuppressWarnings("unused") // TODO: not sure how to handle jdbc execution for now
	private static final ListeningExecutorService executor = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(4));

	private final JdbcConfiguration config;
	
	private final StringWithVars query;
	private final String queryWithPlaceholders;
	private final JdbcConnectionProvider provider;
	private final Path resultField;
	
	public JdbcQuery(JdbcConfiguration config, JdbcConnectionProvider provider, StringWithVars query, Path resultPath) {
		this.config = config;
		this.query = query;
		this.queryWithPlaceholders = query.withPlaceholder("?");
		this.provider = provider;
		this.resultField = resultPath;
	}

	@Override
	public MutableData call(MutableData data) {
		return run(data);
	}

	private MutableData run(MutableData data) {
				
		try (Connection connection = provider.getConnection()) {

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
			}
			
		} catch (Throwable t) {
			
			t.printStackTrace();
			throw unchecked(t);
		}
		
		return data;
	}
	
	private void handleKeys(ResultSet result, Collection<Content> keys) throws SQLException {
		ResultSetMetaData meta = result.getMetaData();
		while (result.next()) {
			keys.add(keyToContent(meta, result, 1));
		}
	}
	
	private Content keyToContent(ResultSetMetaData meta, ResultSet result, int column) throws SQLException {
		
		// see http://docs.oracle.com/javase/6/docs/api/constant-values.html#java.sql.Types.STRUCT
		
		switch (meta.getColumnType(column)) {
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
			if (result.getBoolean(column)) {
				return trueValue();
			} else {
				return falseValue();
			}
		default:
			throw runtime("don't know how to handle column type [%d] / [%s]", meta.getColumnType(column), meta.getColumnTypeName(column));
		}
	}
	
	private void handleResultSet(ResultSet result, MutableData data) throws SQLException {

		ResultSetMetaData meta = result.getMetaData();
		String tableName = meta.getTableName(1).toLowerCase();
		
		Path p = resultField.isEmpty() ? path(tableName) : resultField;
		
		MutableData list = data.createListAt(p);
		int columnCount = meta.getColumnCount();
		MutableData item;
		
		while (result.next()) {
			item = list.createMapAt(path(index(result.getRow() - 1)));
			for (int column = 1; column < columnCount + 1; column++) {
				String key = meta.getColumnLabel(column).toLowerCase();
				putResult(item, path(key), meta, result, column);
			}
		}
		
	}
	
	private void putResult(MutableData item, Path path, ResultSetMetaData meta, ResultSet result, int column) throws IllegalArgumentException, SQLException {

		// see http://docs.oracle.com/javase/6/docs/api/constant-values.html#java.sql.Types.STRUCT
		
		switch (meta.getColumnType(column)) {
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
		case Types.BOOLEAN:
			if (result.getBoolean(column)) {
				item.put(path, trueValue());
				return;
			} else {
				item.put(path, falseValue());
				return;
			}
		default:
			switch (meta.getColumnTypeName(column)) {
			case "json":
				try {
					@SuppressWarnings("unchecked")
					Map<String,Object> m = json.readValue(result.getString(column), Map.class);
					item.put(path, MutableMemoryData.createFromMap(m));
				} catch (IOException e) {
					throw unchecked(e);
				}
				return;
			case "bool":
				if (result.getBoolean(column)) {
					item.put(path, trueValue());
					return;
				} else {
					item.put(path, falseValue());
					return;
				}
			default:
				throw runtime("don't know how to handle column type [%d] / [%s]", meta.getColumnType(column), meta.getColumnTypeName(column));
			}
		}
	}
	
	private static final ObjectMapper json = new ObjectMapper();
}
