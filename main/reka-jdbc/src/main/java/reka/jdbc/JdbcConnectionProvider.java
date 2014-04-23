package reka.jdbc;

import java.sql.Connection;
import java.sql.SQLException;

public interface JdbcConnectionProvider extends AutoCloseable {
	public Connection getConnection() throws SQLException;
}
