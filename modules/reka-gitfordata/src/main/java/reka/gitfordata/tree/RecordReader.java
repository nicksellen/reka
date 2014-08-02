package reka.gitfordata.tree;

import static com.google.common.base.Preconditions.checkArgument;
import static reka.util.Util.runtime;
import static reka.util.Util.unchecked;

import java.io.ByteArrayInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;

import reka.api.content.Contents;
import reka.api.data.storage.DataStorage;
import reka.gitfordata.tree.record.DefaultContentRecord;
import reka.gitfordata.tree.record.builders.CollectionRecordBuilder;
import reka.gitfordata.tree.record.builders.CommitRecordBuilder;

public class RecordReader {
	
	private final DataStorage storage;
	
	public RecordReader(DataStorage storage) {
		this.storage = storage;
	}

	public ObjectIdAndRecord<? extends Record> get(ObjectId id) {
		return ObjectIdAndRecord.from(id, getRecord(id));
	}
	
	public <V extends Record> ObjectIdAndRecord<V> get(ObjectId id, Class<V> klass) {
		return ObjectIdAndRecord.from(id, getRecord(id, klass));
	}

	@SuppressWarnings("unchecked")
	public <V extends Record> V getRecord(ObjectId objectId, Class<V> klass) {
		Record record = getRecord(objectId);
		checkArgument(klass.isInstance(record), "this is not a %s", klass);
		return (V) record;
	}
	
	public Record getRecord(ObjectId objectId) {
	
		byte[] bytes = storage.get(objectId.bytes());

		if (bytes == null) {
			throw new RuntimeException("Could not find object for [" + objectId + "]");
		}

		DataInput in = new DataInputStream(new ByteArrayInputStream(bytes));
		
		try {
			Record.Type type = Record.Type.fromIdentifier(in.readByte());

			switch (type) {
			case COLLECTION:
				return CollectionRecordBuilder.in(in);
			case COMMIT:
				return CommitRecordBuilder.in(in);
			case CONTENT:
				return DefaultContentRecord.fromContent(Contents.in(in));
			default:
				throw runtime("Unknown type [" + type + "]");
			}

		} catch (IOException e) {
			throw unchecked(e);
		}
	}
	

}
