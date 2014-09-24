package reka.jdbc;

import reka.api.data.MutableData;
import reka.core.setup.StatusDataProvider;

public class JdbcStatusProvider implements StatusDataProvider {

	private final JdbcConnectionProvider pool;
	
	public JdbcStatusProvider(JdbcConnectionProvider pool) {
		this.pool = pool;
	}
	
	@Override
	public boolean up() {
		return true;
	}

	@Override
	public void statusData(MutableData data) {
	}

}
