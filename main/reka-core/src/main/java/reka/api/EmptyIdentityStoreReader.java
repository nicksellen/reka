package reka.api;

import java.util.Optional;

public class EmptyIdentityStoreReader implements IdentityStoreReader {

	public static IdentityStoreReader INSTANCE = new EmptyIdentityStoreReader();
	
	private EmptyIdentityStoreReader() {
	}
	
	@Override
	public <T> T get(IdentityKey<T> key) {
		return null;
	}

	@Override
	public <T> Optional<T> lookup(IdentityKey<T> key) {
		return Optional.empty();
	}
	
}