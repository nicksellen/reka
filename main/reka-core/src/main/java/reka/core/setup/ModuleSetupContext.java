package reka.core.setup;

import java.util.Optional;

import reka.api.IdentityKey;
import reka.api.IdentityStore;
import reka.api.IdentityStoreReader;

public class ModuleSetupContext implements IdentityStore {

	private final IdentityStore store;
	
	public ModuleSetupContext(IdentityStore store) {
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
	public <T> T put(IdentityKey<T> key, T value) {
		return store.put(key, value);
	}

	@Override
	public <T> Optional<T> remove(IdentityKey<T> key) {
		return store.remove(key);
	}

	@Override
	public IdentityStoreReader immutable() {
		return store.immutable();
	}

	@Override
	public <T> boolean has(IdentityKey<T> key) {
		return store.has(key);
	}

}
