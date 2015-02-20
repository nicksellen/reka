package reka.exec;

public class SshConfig {

	private final String hostname;
	private final int port;
	private final String user;
	
	public SshConfig(String hostname, int port, String user) {
		this.hostname = hostname;
		this.port = port;
		this.user = user;
	}

	public String hostname() {
		return hostname;
	}

	public int port() {
		return port;
	}

	public String user() {
		return user;
	}
	
}
