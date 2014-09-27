package reka.jdbc;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import reka.api.data.MutableData;

public interface JdbcConnectionProvider extends AutoCloseable {
	Connection getConnection() throws SQLException;
	void finished(Connection connection) throws SQLException;
	DataSource dataSource();
	void writeStats(MutableData data);
}
