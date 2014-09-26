package reka.http;

import static java.lang.String.format;

public class HostAndPort {

	private final String host;
	private final int port;
	
	public HostAndPort(String host, int port) {
		this.host = host;
		this.port = port;
	}
	
	public String host() {
		return host;
	}
	
	public int port() {
		return port;
	}
	
	@Override
	public String toString() {
		return format("%s:%s", host, port);
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((host == null) ? 0 : host.hashCode());
		result = prime * result + port;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		HostAndPort other = (HostAndPort) obj;
		if (host == null) {
			if (other.host != null)
				return false;
		} else if (!host.equals(other.host))
			return false;
		if (port != other.port)
			return false;
		return true;
	}
	
}