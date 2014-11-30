package reka;

public class IdentityAndVersion {
	
	public static IdentityAndVersion create(String identity, int version) {
		return new IdentityAndVersion(identity, version);
	}
	
	private final String identity;
	private final int version;
	
	private IdentityAndVersion(String identity, int version) {
		this.identity = identity;
		this.version = version;
	}
	
	public String identity() {
		return identity;
	}

	public int version() {
		return version;
	}
	
}
