package reka.api;

public interface IdentityStore {
	<T> void put(IdentityKey<T> key, T value);
	<T> T get(IdentityKey<T> key);
}