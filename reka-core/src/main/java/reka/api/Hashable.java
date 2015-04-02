package reka.api;

import com.google.common.hash.Hasher;

public interface Hashable {
	public Hasher hash(Hasher hasher);
}
