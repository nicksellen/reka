package reka.net;

import static java.lang.String.format;
import static reka.util.Util.runtime;
import static reka.util.Util.unchecked;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Optional;

import reka.util.Hashable;

import com.google.common.hash.Hasher;

public class NetSettings implements Hashable {

	public static NetSettings http(int port, String host) {
		return new NetSettings(port, Optional.of(host), Type.HTTP, null);
	}
	
	public static NetSettings https(int port, String host, TlsSettings sslSettings) {
		return new NetSettings(port, Optional.of(host), Type.HTTP, sslSettings);
	}
	
	public static NetSettings ws(int port, String host) {
		return new NetSettings(port, Optional.of(host), Type.WEBSOCKET, null);
	}
	
	public static NetSettings wss(int port, String host, TlsSettings sslSettings) {
		return new NetSettings(port, Optional.of(host), Type.WEBSOCKET, sslSettings);
	}
	
	public static NetSettings socket(int port) {
		return new NetSettings(port, Optional.empty(), Type.SOCKET, null);
	}
	
	public static NetSettings socketSsl(int port, TlsSettings sslSettings) {
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
	private final TlsSettings tlsSettings;
	
	private NetSettings(int port, Optional<String> host, Type type, TlsSettings sslSettings) {
		this.port = port;
		this.host = host;
		this.type = type;
		this.tlsSettings = sslSettings;
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
		return tlsSettings != null;
	}
	
	public TlsSettings tlsSettings() {
		return tlsSettings;
	}
	
	@Override
	public String toString() {
		return format("%s://%s:%s", isSsl() ? "https" : "http", host, port);
	}

	@Override
	public Hasher hash(Hasher hasher) {
		hasher.putInt(port);
		hasher.putBoolean(host.isPresent());
		if (host.isPresent()) {
			hasher.putString(host.get(), StandardCharsets.UTF_8);
		}
		hasher.putString(type.toString(), StandardCharsets.UTF_8);
		hasher.putBoolean(tlsSettings != null);
		if (tlsSettings != null) {
			tlsSettings.hash(hasher);
		}
		return hasher;
	}
	
	public static class TlsSettings implements Hashable {
		
		private final File certChainFile;
		private final File keyFile;
		
		public TlsSettings(File certChainFile, File keyFile) {
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
		public Hasher hash(Hasher hasher) {
			try {
				hasher.putBytes(Files.readAllBytes(certChainFile.toPath()));
				hasher.putBytes(Files.readAllBytes(keyFile.toPath()));
			} catch (IOException e) {
				throw unchecked(e);
			}
			return hasher;
		}
		
	}
	
}
