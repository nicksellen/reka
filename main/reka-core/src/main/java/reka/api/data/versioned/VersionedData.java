package reka.api.data.versioned;

import reka.api.data.Data;

public interface VersionedData extends Data {
	
	public static interface DataVersion {
		byte[] id();
		String text();
	}
	
	DataVersion version(); 
	
	Iterable<ContentChange> changes();
	Iterable<ContentChange> changesSince(DataVersion from);
	Iterable<ContentChange> changesUpTo(DataVersion to);
	Iterable<ContentChange> changesFor(DataVersion to);
	Iterable<ContentChange> changesBetween(DataVersion from, DataVersion to);
	
}
