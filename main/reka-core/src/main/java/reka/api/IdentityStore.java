package reka.api;

import java.util.Optional;

public interface IdentityStore {
	<T> void put(IdentityKey<T> key, T value);
	<T> Optional<T> get(IdentityKey<T> key);
}