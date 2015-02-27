package reka.api;

import static java.lang.System.identityHashCode;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ConcurrentIdentityStore implements IdentityStore {
	
	private final ConcurrentMap<Integer, Object> map = new ConcurrentHashMap<>();
	
	@Override
	public <T> void put(IdentityKey<T> key, T value) {
		map.put(identityHashCode(key), value);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <T> T get(IdentityKey<T> key) {
		return (T) map.get(identityHashCode(key));
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> Optional<T> lookup(IdentityKey<T> key) {
		return Optional.ofNullable((T) map.get(identityHashCode(key)));
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> Optional<T> remove(IdentityKey<T> key) {
		return Optional.ofNullable((T) map.remove(identityHashCode(key)));
	}

	@Override
	public IdentityStoreReader immutable() {
		return ImmutableIdentityStore.from(map);
	}
	
}