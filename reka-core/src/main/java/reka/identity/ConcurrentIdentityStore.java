package reka.identity;

import static java.lang.System.identityHashCode;
import static reka.util.Util.runtime;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ConcurrentIdentityStore implements IdentityStore {
	
	public static IdentityStore create() {
		return new ConcurrentIdentityStore();
	}
	
	public static IdentityStore createFrom(IdentityStoreReader incoming) {
		ConcurrentIdentityStore store = new ConcurrentIdentityStore();
		if (incoming instanceof ImmutableIdentityStore) {
			ImmutableIdentityStore from = (ImmutableIdentityStore) incoming;
			store.map.putAll(from.map);	
		} else if (incoming instanceof ConcurrentIdentityStore) {
			ConcurrentIdentityStore from = (ConcurrentIdentityStore) incoming;
			store.map.putAll(from.map);
		} else {
			throw runtime("sorry, can only create from %s or %s, not %s", ImmutableIdentityStore.class, ConcurrentIdentityStore.class, incoming.getClass());
		}
		return store;
	}
	
	private ConcurrentIdentityStore() {
	}
	
	private final ConcurrentMap<Integer, Object> map = new ConcurrentHashMap<>();
	
	@Override
	public <T> T put(IdentityKey<T> key, T value) {
		map.put(identityHashCode(key), value);
		return value;
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

	@Override
	public <T> boolean has(IdentityKey<T> key) {
		return map.containsKey(identityHashCode(key));
	}
	
}