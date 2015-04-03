package reka.jdbc;

import java.sql.Connection;

import reka.data.MutableData;
import reka.module.setup.StatusDataProvider;

public class JdbcStatusProvider implements StatusDataProvider {

	private static final String CHECK_CONNECTION_SQL = "select 1";
	
	private final String url;
	private final JdbcConnectionProvider pool;
	
	public JdbcStatusProvider(String url, JdbcConnectionProvider pool) {
		this.url = url;
		this.pool = pool;
	}
	
	@Override
	public boolean up() {
		try (Connection connection = pool.getConnection()) {
			connection.prepareCall(CHECK_CONNECTION_SQL).execute();
			return true;
		} catch (Throwable t) {
			return false;
		}
	}

	@Override
	public void statusData(MutableData data) {
		pool.writeStats(data);
		data.putString("url", url);
	}

}
