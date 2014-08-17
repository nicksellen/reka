package reka.api.data;

import reka.api.Path;

public interface MutableData extends Data, DataMutation<MutableData> {
	
	void clear();
	
	MutableData mutableCopy();
	MutableData mutableAt(Path path);

	default MutableData merge(Data other) {
		if (this == other) return this;
		other.forEachContent((path, content) -> put(path, content));
		return this;
	}
	
	// TODO: actually set a immutable flag somewhere....
	default Data immutable() {
		return this;
	}
	
	@Override
	default Data copy() {
		return mutableCopy().immutable();
	}
}