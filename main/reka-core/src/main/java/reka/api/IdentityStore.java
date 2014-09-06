package reka.api;

import java.util.Optional;

public interface IdentityStore {
	<T> void put(IdentityKey<T> key, T value);
	<T> T get(IdentityKey<T> key);
	<T> Optional<T> lookup(IdentityKey<T> key);
}