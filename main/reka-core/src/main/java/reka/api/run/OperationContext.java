package reka.api.run;

import java.util.Optional;

import reka.api.IdentityKey;
import reka.api.IdentityStoreReader;

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
