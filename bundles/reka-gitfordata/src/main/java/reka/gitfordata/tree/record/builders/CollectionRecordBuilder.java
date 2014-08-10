package reka.gitfordata.tree.record.builders;

import static reka.util.Util.runtime;
import static reka.util.Util.unchecked;

import java.io.DataInput;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import reka.api.Path.PathElement;
import reka.api.Path.PathElements;
import reka.gitfordata.tree.ObjectId;
import reka.gitfordata.tree.ObjectIdAndRecord;
import reka.gitfordata.tree.Record;
import reka.gitfordata.tree.RecordBuilder;
import reka.gitfordata.tree.record.CollectionRecord;

public class CollectionRecordBuilder implements RecordBuilder<CollectionRecord> {

	private final Map<PathElement, RecordBuilder.BuilderOrObjectId> children = new TreeMap<>();

	private CollectionRecord.Type type; // 0 (map) or 1 (list), we set based on the first item coming in
	
	public static CollectionRecord in(DataInput in) {
		try {
			CollectionRecord.Type type = CollectionRecord.Type.fromByte(in.readByte());
			int childCount = in.readInt();
			Map<PathElement, ObjectId> children = new TreeMap<>();
			for (int i = 0; i < childCount; i++) {
				PathElement element = type == CollectionRecord.Type.MAP ?
					PathElements.name(in.readUTF()) : PathElements.index(i);
				byte[] bytes = new byte[ObjectId.SIZE];
				in.readFully(bytes);
				children.put(element, ObjectId.fromBytes(bytes));
			}
			return new CollectionRecord(children);
		} catch (IOException e) {
			throw unchecked(e);
		}
	}

	public CollectionRecordBuilder add(PathElement element, RecordBuilder<?> item) {
		checkAndSetType(element);
		children.put(element, new RecordBuilder.BuilderOrObjectId(item));
		return this;
	}

	public CollectionRecordBuilder add(PathElement element, ObjectId item) {
		checkAndSetType(element);
		children.put(element, new RecordBuilder.BuilderOrObjectId(item));
		return this;
	}

	public CollectionRecordBuilder remove(PathElement element) {
		checkAndSetType(element);
		children.remove(element);
		return this;
	}

	public boolean isEmpty() {
		return children.isEmpty();
	}

	public RecordBuilder.BuilderOrObjectId get(PathElement element) {
		checkAndSetType(element);
		return children.get(element);
	}
	
	private void checkAndSetType(PathElement element) {
		if (children.isEmpty()) {
			type = element.isKey() ? CollectionRecord.Type.MAP : CollectionRecord.Type.LIST;
		} else {
			if (element.isKey() && type != CollectionRecord.Type.MAP) {
				throw runtime("this builder is for lists, not maps");
			} else if (element.isIndex() && type != CollectionRecord.Type.LIST) {
				throw runtime("this builder is for maps, not lists");
			}
		}
	}

	@Override
	public CollectionRecord build(RecordCollector collector) {

		Map<PathElement, ObjectId> treeChildren = new TreeMap<>();
		
		for (Entry<PathElement, BuilderOrObjectId> entry : children.entrySet()) {

			if (entry.getValue().isEmptyTreeBuilder()) {

				// ignore empty Tree.Builder as quick as possible

			} else {
				if (entry.getValue().isObjectId()) {
					// TODO: how do I collect this? or maybe I don't need to...
					treeChildren.put(entry.getKey(), entry.getValue().objectId());
				} else {
					Record record = entry.getValue().builder().build(collector);
					if (!(record instanceof CollectionRecord && ((CollectionRecord) record).children().isEmpty())) {
						ObjectIdAndRecord<?> identifiable = ObjectIdAndRecord.from(record);
						collector.collect(identifiable.objectId(), identifiable.record());
						treeChildren.put(entry.getKey(), identifiable.objectId());
					}
				}
			}
		}
		
		return new CollectionRecord(treeChildren);
	}

}
