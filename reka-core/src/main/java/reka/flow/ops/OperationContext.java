package reka.flow.ops;

import java.util.Optional;

import reka.identity.IdentityKey;
import reka.identity.IdentityStoreReader;

public class OperationContext implements IdentityStoreReader {

	private final IdentityStoreReader store;
	
	public OperationContext(IdentityStoreReader store) {
		this.store = store;
	}
	
	@Override
	public <T> T get(IdentityKey<T> key) {
		return store.get(key);
	}

	@Override
	public <T> Optional<T> lookup(IdentityKey<T> key) {
		return store.lookup(key);
	}

	@Override
	public <T> boolean has(IdentityKey<T> key) {
		return store.has(key);
	}
	
}
