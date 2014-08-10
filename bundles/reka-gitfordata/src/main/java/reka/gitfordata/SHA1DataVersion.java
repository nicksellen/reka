package reka.gitfordata;

import reka.api.data.versioned.VersionedData.DataVersion;
import reka.gitfordata.tree.ObjectId;

public class SHA1DataVersion implements DataVersion {
	
	private static final SHA1DataVersion INITIAL = new SHA1DataVersion(ObjectId.fromBytes(new byte[20]));
	
	public static SHA1DataVersion initial() {
		return INITIAL;
	}
	
	public static SHA1DataVersion fromText(String text) {
		return new SHA1DataVersion(ObjectId.fromHex(text));
	}
	
	private final ObjectId id;
	
	public SHA1DataVersion(ObjectId id) {
		this.id = id;
	}

	@Override
	public byte[] id() {
		return id.bytes();
	}
	
	@Override
	public String text() {
		return id.hex();
	}
	
	public ObjectId commitId() {
		return id;
	}

}
