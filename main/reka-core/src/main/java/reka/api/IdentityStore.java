package reka.api;

import java.util.Optional;

public interface IdentityStore extends IdentityStoreReader {
	
	public static IdentityStore createConcurrentIdentityStore() {
		return new ConcurrentIdentityStore();
	}
	
	public static ImmutableIdentityStore.Builder immutableBuilder() {
		return new ImmutableIdentityStore.Builder();
	}
	
	public static IdentityStoreReader emptyReader() {
		return EmptyIdentityStoreReader.INSTANCE;
	}
	
	<T> void put(IdentityKey<T> key, T value);
	<T> Optional<T> remove(IdentityKey<T> key);
	
	IdentityStoreReader immutable();
	
}