package reka.jdbc;

import reka.api.data.MutableData;
import reka.core.setup.StatusDataProvider;

public class JdbcStatusProvider implements StatusDataProvider {

	private final String url;
	private final JdbcConnectionProvider pool;
	
	public JdbcStatusProvider(String url, JdbcConnectionProvider pool) {
		this.url = url;
		this.pool = pool;
	}
	
	@Override
	public boolean up() {
		return true;
	}

	@Override
	public void statusData(MutableData data) {
		pool.writeStats(data);
		data.putString("url", url);
	}

}
