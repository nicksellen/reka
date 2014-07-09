package reka.http.server;

import java.util.Objects;

public class HttpSettings {
	
	public static enum Type { HTTP, WEBSOCKET };
	public static enum Security { NONE, SSL };
	
	private final int port;
	private final String host;
	private final Type type;
	private final Security security;
	private final int applicationVersion; // application version
	
	public HttpSettings(int port, String host, Type type, Security security, int applicationVersion) {
		this.port = port;
		this.host = host;
		this.type = type;
		this.security = security;
		this.applicationVersion = applicationVersion;
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

	public int applicationVersion() {
		return applicationVersion;
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
