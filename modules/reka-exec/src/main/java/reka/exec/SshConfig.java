package reka.exec;

public class SshConfig {

	private final String hostname;
	private final int port;
	private final String user;
	private final char[] privateKey;
	private final char[] publicKey;
	private final char[] passphrase;
	
	public SshConfig(String hostname, int port, String user, char[] privateKey, char[] publicKey, char[] passphase) {
		this.hostname = hostname;
		this.port = port;
		this.user = user;
		this.privateKey = privateKey;
		this.publicKey = publicKey;
		this.passphrase = passphase;
	}

	public String hostname() {
		return hostname;
	}

	public int port() {
		return port;
	}

	public String user() {
		return user;
	}
	
	public char[] privateKey() {
		return privateKey;
	}
	
	public String privateKeyAsString() {
		return new String(privateKey);
	}
	
	public char[] publicKey() {
		return publicKey;
	}

	public String publicKeyAsString() {
		return new String(publicKey);
	}
	
	public char[] passphrase() {
		return passphrase;
	}
	
}
