package reka.net;

import static java.lang.String.format;
import static reka.util.Util.runtime;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Optional;

public class NetSettings {

	public static NetSettings http(int port, String host) {
		return new NetSettings(port, Optional.of(host), Type.HTTP, null);
	}
	
	public static NetSettings https(int port, String host, SslSettings sslSettings) {
		return new NetSettings(port, Optional.of(host), Type.HTTP, sslSettings);
	}
	
	public static NetSettings ws(int port, String host) {
		return new NetSettings(port, Optional.of(host), Type.WEBSOCKET, null);
	}
	
	public static NetSettings wss(int port, String host, SslSettings sslSettings) {
		return new NetSettings(port, Optional.of(host), Type.WEBSOCKET, sslSettings);
	}
	
	public static NetSettings socket(int port) {
		return new NetSettings(port, Optional.empty(), Type.SOCKET, null);
	}
	
	public static NetSettings socketSsl(int port, SslSettings sslSettings) {
		return new NetSettings(port, Optional.empty(), Type.SOCKET, sslSettings);
	}
	
	public static enum Type {
		
		SOCKET, HTTP, WEBSOCKET;
		
		public String protocolString(boolean ssl) {
			switch (this) {
			case HTTP:
				if (ssl) {
					return "https";
				} else {
					return "http";
				}
			case WEBSOCKET:
				if (ssl) {
					return "wss";
				} else {
					return "ws";
				}
			case SOCKET:
				if (ssl) {
					return "ssl socket";
				} else {
					return "socket";
				}
			default:
				throw runtime("unknown protocol for %s", this);
			}
		}
		
	};

	private final Type type;
	private final int port;
	private final Optional<String> host;
	private final SslSettings sslSettings;
	
	private NetSettings(int port, Optional<String> host, Type type, SslSettings sslSettings) {
		this.port = port;
		this.host = host;
		this.type = type;
		this.sslSettings = sslSettings;
	}

	public int port() {
		return port;
	}
	
	public Optional<String> host() {
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
	
	@Override
	public String toString() {
		return format("%s://%s:%s", isSsl() ? "https" : "http", host, port);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((host == null) ? 0 : host.hashCode());
		result = prime * result + port;
		result = prime * result + ((sslSettings == null) ? 0 : sslSettings.hashCode());
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
		NetSettings other = (NetSettings) obj;
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
				if (a.length() != b.length()) {
					return false;
				}
				return isEqual(Files.newInputStream(a.toPath()), Files.newInputStream(b.toPath()));
			} catch (IOException e) {
				return false;
			}
		}
		
		private static boolean isEqual(InputStream i1, InputStream i2) throws IOException {
			byte[] buf1 = new byte[64 * 1024];
			byte[] buf2 = new byte[64 * 1024];
			DataInputStream d2 = new DataInputStream(i2);
			try {
				int len;
				while ((len = i1.read(buf1)) > 0) {
					d2.readFully(buf2, 0, len);
					for (int i = 0; i < len; i++) {
						if (buf1[i] != buf2[i]) {
							return false;
						}
					}
				}
				return d2.read() < 0; // is the end of the second file also.
			} catch (EOFException ioe) {
				return false;
			} finally {
				i1.close();
				i2.close();
				d2.close();
			}
		}
		
	}
	
}
