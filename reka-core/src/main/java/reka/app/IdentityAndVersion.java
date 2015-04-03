package reka.app;

import java.util.UUID;

import reka.identity.Identity;

public class IdentityAndVersion {
	
	public static IdentityAndVersion create(Identity identity, int version) {
		return new IdentityAndVersion(identity, version);
	}
	
	private final Identity identity;
	private final int version;
	
	private IdentityAndVersion(Identity identity, int version) {
		this.identity = identity;
		this.version = version;
	}
	
	public Identity identity() {
		return identity;
	}

	public int version() {
		return version;
	}

	public static IdentityAndVersion tmp() {
		return create(Identity.create(String.format("tmp/%s", UUID.randomUUID().toString())), 1);
	}
	
}
