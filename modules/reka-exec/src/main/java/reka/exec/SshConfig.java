package reka.exec;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import reka.api.Hashable;

import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;

public class SshConfig implements Hashable {

	private final String hostname;
	private final int port;
	private final String user;
	private final char[] privateKey;
	private final char[] publicKey;
	private final char[] passphrase;
	private final String hostkey;
	
	public SshConfig(String hostname, int port, String user, char[] privateKey, char[] publicKey, char[] passphase, String hostkey) {
		this.hostname = hostname;
		this.port = port;
		this.user = user;
		this.privateKey = privateKey;
		this.publicKey = publicKey;
		this.passphrase = passphase;
		this.hostkey = hostkey;
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
		// copying it because some uses will blank it out
		// probably a good idea to do so but we need to reuse this for reconnections
		return Arrays.copyOf(passphrase, passphrase.length);
	}
	
	public String hostkey() {
		return hostkey;
	}
	
	public byte[] sha1() {
		return hash(Hashing.sha1().newHasher()).hash().asBytes();
	}

	@Override
	public Hasher hash(Hasher hasher) {
		
		hasher.putString(hostname, StandardCharsets.UTF_8)
			  .putInt(port)
			  .putString(user, StandardCharsets.UTF_8);
		
		putChars(hasher, privateKey);
		putChars(hasher, publicKey);
		putChars(hasher, passphrase);
		
		hasher.putString(hostkey, StandardCharsets.UTF_8);
		
		return hasher;
	}
	
	private static void putChars(Hasher hasher, char[] chars) {
		for (int i = 0; i < chars.length; i++) {
			hasher.putChar(chars[i]);
		}
	}
	
}
