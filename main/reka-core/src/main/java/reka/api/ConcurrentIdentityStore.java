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
	
	@Override
	@SuppressWarnings("unchecked")
	public <T> Optional<T> get(IdentityKey<T> key) {
		return Optional.ofNullable((T) map.get(identityHashCode(key)));
	}
	
}