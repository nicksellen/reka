package reka.jdbc;

public class JdbcConfiguration {
	
	public final boolean returnGeneratedKeys;

	public JdbcConfiguration(boolean returnGeneratedKeys) {
		this.returnGeneratedKeys = returnGeneratedKeys;
	}

}
