package reka.http.server;

import java.util.Objects;

public class HttpSettings {
	
	public static enum Type { HTTP, WEBSOCKET };
	public static enum Security { NONE, SSL };
	
	private final int port;
	private final String host;
	private final Type type;
	private final Security security;
	
	public HttpSettings(int port, String host, Type type, Security security) {
		this.port = port;
		this.host = host;
		this.type = type;
		this.security = security;
	}

	public int port() {
		return port;
	}
	
	public String host() {
		return host;
	}
	
	public Type type() {
		return type;
	}
	
	public Security security() {
		return security;
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(port, host, type, security);
	}
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof HttpSettings)) {
			return false;
		}
		HttpSettings other = (HttpSettings) obj;
		return host.equals(other.host) 
			&& security.equals(other.security)
			&& type.equals(other.type)
			&& port == other.port;
	}
	
}
