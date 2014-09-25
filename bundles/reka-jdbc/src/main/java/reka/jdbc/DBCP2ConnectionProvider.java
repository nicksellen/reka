package reka.jdbc;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.apache.commons.dbcp2.ConnectionFactory;
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
	
	private final ConnectionFactory factory;
	private final PoolableConnectionFactory poolableConnectionFactory;
	private final ObjectPool<PoolableConnection> connectionPool;
	private final PoolingDataSource<PoolableConnection> dataSource;

	public DBCP2ConnectionProvider(String url, String username, String password) {
		factory = new DriverManagerConnectionFactory(url, username, password);
		poolableConnectionFactory = new PoolableConnectionFactory(factory, null);
		connectionPool = new GenericObjectPool<>(poolableConnectionFactory);
		poolableConnectionFactory.setPool(connectionPool);
		dataSource = new PoolingDataSource<>(connectionPool);
	}

	@Override
	public void close() throws Exception {
		log.info("closing connection pool");
		connectionPool.close();
	}

	@Override
	public Connection getConnection() throws SQLException {
		return dataSource.getConnection();
	}
	
	@Override
	public DataSource dataSource() {
		return dataSource;
	}
	
	public void writeStats(MutableData data) {
		int active = connectionPool.getNumActive();
		if (active >= 0) data.putInt("active", active);
		
		int idle = connectionPool.getNumIdle();
		if (idle >= 0) data.putInt("idle", idle);
	}

}
