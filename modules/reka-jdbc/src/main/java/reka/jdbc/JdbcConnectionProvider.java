package reka.jdbc;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

public interface JdbcConnectionProvider extends AutoCloseable {
	Connection getConnection() throws SQLException;
	DataSource dataSource();
}
