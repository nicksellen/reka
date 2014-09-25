package reka.jdbc;

import reka.api.data.MutableData;
import reka.core.setup.StatusDataProvider;

public class JdbcStatusProvider implements StatusDataProvider {

	private final DBCP2ConnectionProvider pool;
	
	public JdbcStatusProvider(DBCP2ConnectionProvider pool) {
		this.pool = pool;
	}
	
	@Override
	public boolean up() {
		return true;
	}

	@Override
	public void statusData(MutableData data) {
		pool.writeStats(data);
	}

}
