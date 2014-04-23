package reka.gitfordata.tree.record;

import static reka.util.Util.runtime;
import static reka.util.Util.unchecked;

import java.io.DataOutput;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;

import reka.api.Path.PathElement;
import reka.gitfordata.tree.ObjectId;
import reka.gitfordata.tree.Record;
import reka.gitfordata.tree.record.builders.CollectionRecordBuilder;

public final class CollectionRecord implements Record {

	public static enum Type { 
		MAP(0), LIST(1);
		private final byte b;
		Type(int val) {
			this.b = (byte) val;
		}
		public static Type fromByte(byte b) {
			int v = (int)b;
			switch (v) {
			case 0: return MAP;
			case 1: return LIST;
			default:
				throw runtime("invalid MapOrList.Type %d", v);
			}
		}
	}
	
	private final Type type;
	private final Map<PathElement,ObjectId> children;
	
	public CollectionRecord(Map<PathElement,ObjectId> children) {
		this.children = children;
		if (children.isEmpty() || children.entrySet().iterator().next().getKey().isKey()) {
			type = CollectionRecord.Type.MAP;
		} else {
			type = CollectionRecord.Type.LIST;
		}
	}

	public CollectionRecordBuilder toBuilder() {
		CollectionRecordBuilder builder = new CollectionRecordBuilder();
		for (Entry<PathElement, ObjectId> child : children.entrySet()) {
			builder.add(child.getKey(), child.getValue());
		}
		return builder;
	}
	
	@Override
	public void out(DataOutput out) {
		try {
			out.writeByte(type.b);
			out.writeInt(children.size());
			for (Entry<PathElement, ObjectId> child : children.entrySet()) {
				PathElement element = child.getKey();
				if (element.isKey()) {
					out.writeUTF(element.name());
				}
				out.write(child.getValue().bytes());
			}
		} catch (IOException e) {
			throw unchecked(e);
		}
		
	}
	
	public Type collectionType() {
		return type;
	}

	@Override
	public Record.Type recordType() {
		return Record.Type.COLLECTION;
	}

	@Override
	public Map<PathElement,ObjectId> children() {
		return children;
	}
	
}
