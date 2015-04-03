package reka.identity;

import static reka.util.Util.runtime;

import java.util.Optional;

public interface IdentityStoreReader {
	
	<T> boolean has(IdentityKey<T> key);
	<T> T get(IdentityKey<T> key);
	<T> Optional<T> lookup(IdentityKey<T> key);
	
	default <T> T require(IdentityKey<T> key) {
		T value = get(key);
		if (value == null) throw runtime("missing value [%s]", key.name());
		return value;
	}
	
}