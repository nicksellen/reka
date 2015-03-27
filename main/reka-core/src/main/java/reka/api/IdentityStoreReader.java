package reka.api;

import java.util.Optional;

public interface IdentityStoreReader {
	<T> boolean has(IdentityKey<T> key);
	<T> T get(IdentityKey<T> key);
	<T> Optional<T> lookup(IdentityKey<T> key);
}