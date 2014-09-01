package reka.core.bundle;

import java.util.Optional;

import reka.api.data.Data;

public class NetworkInfo implements Comparable<NetworkInfo> {
	
	private final int port;
	private final String protocol;
	private final Data details;
	
	public NetworkInfo(int port, String protocol, Data details) {
		this.port = port;
		this.protocol = protocol;
		this.details = details;
	}
	
	public int port() {
		return port;
	}
	
	public String protocol() {
		return protocol;
	}
	
	public Data details() {
		return details;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		
		Optional<String> host = details.getString("host");
		
		if (host.isPresent()) {
			sb.append(protocol).append("://").append(host.get());
			if (!isDefaultPort()) sb.append(':').append(port);
		} else {
			sb.append(protocol).append(" on port ").append(port);
		}
		
		return sb.toString(); 
	}
	
	public boolean isDefaultPort() {
		return ("https".equals(protocol) && port == 443) || 
			   ("http".equals(protocol) && port == 80) || 
			   ("smtp".equals(protocol) && port == 25);
	}

	@Override
	public int compareTo(NetworkInfo o) {
		if (!protocol.equals(o.protocol)) {
			return protocol.compareTo(o.protocol);
		} else if (port != o.port) {
			return Integer.compare(port, o.port);
		} else {
			return 0;
		}
	}
	
}