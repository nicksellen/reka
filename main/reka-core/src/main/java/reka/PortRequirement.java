package reka;

import java.util.Optional;

public class PortRequirement {
	
	private final int port;
	private final Optional<String> host;
	
	public PortRequirement(int port, Optional<String> host) {
		this.port = port;
		this.host = host;
	}
	
	public int port() {
		return port;
	}
	
	public Optional<String> host() {
		return host;
	}

}
