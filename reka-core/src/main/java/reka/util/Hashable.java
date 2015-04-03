package reka.util;

import com.google.common.hash.Hasher;

public interface Hashable {
	public Hasher hash(Hasher hasher);
}
