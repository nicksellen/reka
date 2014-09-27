package reka.jdbc;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.LinkedBlockingDeque;

import javax.sql.DataSource;

import org.apache.commons.dbcp2.DriverManagerConnectionFactory;
import org.apache.commons.dbcp2.PoolableConnection;
import org.apache.commons.dbcp2.PoolableConnectionFactory;
import org.apache.commons.dbcp2.PoolingDataSource;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.api.data.MutableData;

public class DBCP2ConnectionProvider implements JdbcConnectionProvider {
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	private final PoolableConnectionFactory factory;
	private final ObjectPool<PoolableConnection> pool;
	private final PoolingDataSource<PoolableConnection> ds;

	public DBCP2ConnectionProvider(String url, String username, String password, boolean poolStatements) {
		factory = new PoolableConnectionFactory(new DriverManagerConnectionFactory(url, username, password), null);
		factory.setPoolStatements(poolStatements);
		pool = new GenericObjectPool<>(factory);
		factory.setPool(pool);
		ds = new PoolingDataSource<>(pool);
	}

	@Override
	public void close() throws Exception {
		log.info("closing connection pool");
		pool.close();
	}

	@Override
	public Connection getConnection() throws SQLException {
		return ds.getConnection();
	}
	
	@Override
	public DataSource dataSource() {
		return ds;
	}

	@Override
	public void writeStats(MutableData data) {
		int active = pool.getNumActive();
		if (active >= 0) data.putInt("active", active);
		
		int idle = pool.getNumIdle();
		if (idle >= 0) data.putInt("idle", idle);
	}

	@Override
	public void finished(Connection connection) throws SQLException {
		connection.close();
	}

}
