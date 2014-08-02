package reka.gitfordata.tree;

import static reka.util.Util.runtime;

import java.io.DataOutput;
import java.util.Collections;
import java.util.Map;

import reka.api.Path.PathElement;

public interface Record {
	
	public enum Type {
		COLLECTION(1),
		COMMIT(2),
		CONTENT(3);
		private final byte b;
		private Type(int b) {
			this.b = (byte) b;
		}
		public byte identifier() {
			return b;
		}
		public static Type fromIdentifier(byte b) {
			switch (b) {
			case 1: return COLLECTION;
			case 2: return COMMIT;
			case 3: return CONTENT;
			default:
					throw runtime("unknown record type %d", b);
			}
		}
	}
	
	Type recordType();
	void out(DataOutput out);
	
	default Map<PathElement,ObjectId> children() {
		return Collections.emptyMap();
	}
	
}
