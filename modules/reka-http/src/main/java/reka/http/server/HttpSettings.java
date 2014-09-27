package reka.http.server;

import static java.lang.String.format;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;

public class HttpSettings {
	
	public static enum Type { HTTP, WEBSOCKET };
	
	public static class SslSettings {
		
		private final File certChainFile;
		private final File keyFile;
		
		public SslSettings(File certChainFile, File keyFile) {
			this.certChainFile = certChainFile;
			this.keyFile = keyFile;
		}
		
		public File certChainFile() {
			return certChainFile;
		}
		
		public File keyFile() {
			return keyFile;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ ((certChainFile == null) ? 0 : certChainFile.hashCode());
			result = prime * result
					+ ((keyFile == null) ? 0 : keyFile.hashCode());
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
			SslSettings other = (SslSettings) obj;
			if (certChainFile == null) {
				if (other.certChainFile != null)
					return false;
			} else if (!certChainFile.equals(other.certChainFile))
				return contentsEquals(certChainFile, other.certChainFile);
				
			if (keyFile == null) {
				if (other.keyFile != null)
					return false;
			} else if (!keyFile.equals(other.keyFile))
				return contentsEquals(keyFile, other.keyFile);
			
			return true;
		}

		private boolean contentsEquals(File a, File b) {
			try {
				return Arrays.equals(
						Files.readAllBytes(a.toPath()), 
						Files.readAllBytes(b.toPath()));
			} catch (IOException e) {
				return false;
			}
		}
		
	}
	
	private final int port;
	private final String host;
	private final Type type;
	private final SslSettings sslSettings;
	private final String applicationIdentity;
	private final int applicationVersion;
	
	public static HttpSettings http(int port, String host, Type type, String applicationIdentity, int applicationVersion) {
		return new HttpSettings(port, host, type, null, applicationIdentity, applicationVersion);
	}
	
	public static HttpSettings https(int port, String host, Type type, String applicationIdentity, int applicationVersion, SslSettings sslSettings) {
		return new HttpSettings(port, host, type, sslSettings, applicationIdentity, applicationVersion);
	}
	
	private HttpSettings(int port, String host, Type type, SslSettings sslSettings, String applicationIdentity, int applicationVersion) {
		this.port = port;
		this.host = host;
		this.type = type;
		this.sslSettings = sslSettings;
		this.applicationIdentity = applicationIdentity;
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

	public boolean isSsl() {
		return sslSettings != null;
	}
	
	public SslSettings sslSettings() {
		return sslSettings;
	}

	public int applicationVersion() {
		return applicationVersion;
	}
	
	public String applicationIdentity() {
		return applicationIdentity;
	}
	
	@Override
	public String toString() {
		return format("%s://%s:%s v%s", isSsl() ? "https" : "http", host, port, applicationVersion);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + applicationVersion;
		result = prime * result + ((host == null) ? 0 : host.hashCode());
		result = prime * result + port;
		result = prime * result
				+ ((sslSettings == null) ? 0 : sslSettings.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
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
		HttpSettings other = (HttpSettings) obj;
		if (applicationVersion != other.applicationVersion)
			return false;
		if (host == null) {
			if (other.host != null)
				return false;
		} else if (!host.equals(other.host))
			return false;
		if (port != other.port)
			return false;
		if (sslSettings == null) {
			if (other.sslSettings != null)
				return false;
		} else if (!sslSettings.equals(other.sslSettings))
			return false;
		if (type != other.type)
			return false;
		return true;
	}
	
	
	
}
