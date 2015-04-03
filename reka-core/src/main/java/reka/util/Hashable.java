package reka.util;

import static reka.util.Util.hex;

import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;

public interface Hashable {
	
	Hasher hash(Hasher hasher);
	
	default String sha1hex() {
		return hex(hash(Hashing.sha1().newHasher()).hash().asBytes());
	}
	
}
