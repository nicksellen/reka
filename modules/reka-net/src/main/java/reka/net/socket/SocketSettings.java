package reka.net.socket;

public class SocketSettings {

	private final String applicationIdentity;
	private final int applicationVersion;
	private final int port;
	
	public SocketSettings(String applicationIdentity, int applicationVersion, int port) {
		this.applicationIdentity = applicationIdentity;
		this.applicationVersion = applicationVersion;
		this.port = port;
	}
	
	public String applicationIdentity() {
		return applicationIdentity;
	}
	
	public int applicationVersion() {
		return applicationVersion;
	}

	public int port() {
		return port;
	}
	
}
