package reka.api;

import java.util.Optional;

public interface IdentityStore extends IdentityStoreReader {
	
	public static IdentityStore createConcurrentIdentityStore() {
		return ConcurrentIdentityStore.create();
	}
	
	public static ImmutableIdentityStore.Builder immutableBuilder() {
		return new ImmutableIdentityStore.Builder();
	}
	
	public static IdentityStoreReader emptyReader() {
		return EmptyIdentityStoreReader.INSTANCE;
	}
	
	<T> T put(IdentityKey<T> key, T value);
	<T> Optional<T> remove(IdentityKey<T> key);
	
	IdentityStoreReader immutable();
	
	default <T> T putIfAbsent(IdentityKey<T> key, T value) {
		T existing = get(key);
		if (existing != null) {
			return existing;
		} else {
			put(key, value);
			return value;
		}
	}
	
}