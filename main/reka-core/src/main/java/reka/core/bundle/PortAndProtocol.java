package reka.core.bundle;

import reka.api.data.Data;

public class PortAndProtocol {
	
	private final int port;
	private final String protocol;
	private final Data details;
	
	public PortAndProtocol(int port, String protocol) {
		this(port, protocol, Data.NONE);
	}
	
	public PortAndProtocol(int port, String protocol, Data details) {
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
		sb.append(protocol).append('/').append(port);
		if (details.isPresent()) {
			sb.append(" ");
			sb.append(details.toJson());
		}
		return sb.toString(); 
	}
	
}