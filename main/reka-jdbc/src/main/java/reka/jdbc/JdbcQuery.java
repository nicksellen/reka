package reka.jdbc;

import static reka.api.Path.path;
import static reka.api.Path.PathElements.index;
import static reka.api.content.Contents.falseValue;
import static reka.api.content.Contents.integer;
import static reka.api.content.Contents.longValue;
import static reka.api.content.Contents.trueValue;
import static reka.api.content.Contents.utf8;
import static reka.util.Util.runtime;
import static reka.util.Util.unchecked;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.api.Path;
import reka.api.content.Content;
import reka.api.data.MutableData;
import reka.api.run.SyncOperation;
import reka.core.util.StringWithVars;
import reka.core.util.StringWithVars.Variable;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

public class JdbcQuery implements SyncOperation {

	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger("jdbc-query");
	
	@SuppressWarnings("unused") // TODO: not sure how to handle jdbc execution for now
	private static final ListeningExecutorService executor = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(4));

	private final StringWithVars query;
	private final String queryWithPlaceholders;
	private final JdbcConnectionProvider provider;
	private final Path resultField;
	private final Path entriesPath;
	private final Path firstEntryPath;
	
	public JdbcQuery(JdbcConnectionProvider provider, StringWithVars query, Path resultPath) {
		this.query = query;
		this.queryWithPlaceholders = query.withPlaceholder("?");
		this.provider = provider;
		this.resultField = resultPath;
		this.entriesPath = resultField.add("entries");
		this.firstEntryPath = resultField.add("first");
	}
	
	/*
	public ListenableFuture<MutableData> call(final MutableData data) {
		
		return executor.submit(new Callable<MutableData>(){

			@Override
			public MutableData call() throws Exception {
				return run(data);
			}
			
		});
	}
	*/

	@Override
	public MutableData call(MutableData data) {
		return run(data);
	}

	private MutableData run(MutableData data) {
		
		try (Connection connection = provider.getConnection()){
			
			connection.setAutoCommit(false);

			boolean updates = false;
			int updateCount = 0;
			
			List<Content> keys = new ArrayList<>();
				
			PreparedStatement statement = connection.prepareStatement(
					queryWithPlaceholders, 
					Statement.RETURN_GENERATED_KEYS);
			
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
			t.printStackTrace();
			throw unchecked(t);
		}
		
		return data;
	}
	
	private void handleKeys(ResultSet result, Collection<Content> keys) throws SQLException {
		ResultSetMetaData meta = result.getMetaData();
		while (result.next()) {
			keys.add(resultToContent(meta, result, 1));
		}
	}
	
	private Content resultToContent(ResultSetMetaData meta, ResultSet result, int column) throws SQLException {
		
		// see http://docs.oracle.com/javase/6/docs/api/constant-values.html#java.sql.Types.STRUCT
		
		switch (meta.getColumnType(column)) {
		case Types.LONGNVARCHAR:
		case Types.LONGVARCHAR:
		case Types.CLOB:
		case Types.CHAR:
		case Types.VARCHAR: return utf8(result.getString(column));
		case Types.SMALLINT:
		case Types.INTEGER: return integer(result.getInt(column));
		case Types.BIGINT: return longValue(result.getLong(column));
		case Types.TIMESTAMP: return longValue(result.getTimestamp(column).getTime()); // TODO: how to represent dates?
		case Types.BOOLEAN:
			if (result.getBoolean(column)) {
				return trueValue();
			} else {
				return falseValue();
			}
		default:
			throw runtime("don't know how to handle column type [%d]", meta.getColumnType(column));
		}
	}
	
	private void handleResultSet(ResultSet result, MutableData data) throws SQLException {
		MutableData list = data.createListAt(entriesPath);
		ResultSetMetaData meta = result.getMetaData();
		int columnCount = meta.getColumnCount();
		MutableData item;
		
		while (result.next()) {
			item = list.createMapAt(path(index(result.getRow() - 1)));
			for (int column = 1; column < columnCount + 1; column++) {
				String key = meta.getColumnLabel(column).toLowerCase();
				item.put(path(key), resultToContent(meta, result, column));
			}
		}
		
		list.at(index(0)).forEachContent((p,c) -> {
			data.put(firstEntryPath.add(p), c);
		});
		
	}
	
	public void cancelled() { }
}
