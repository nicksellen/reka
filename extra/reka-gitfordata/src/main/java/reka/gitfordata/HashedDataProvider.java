package reka.gitfordata;

import static java.util.stream.Collectors.toList;
import static reka.util.Util.createEntry;
import static reka.util.Util.unsupported;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.BiConsumer;

import org.codehaus.jackson.JsonGenerator;

import reka.api.Path;
import reka.api.Path.PathElement;
import reka.api.content.Content;
import reka.api.data.Data;
import reka.api.data.versioned.VersionedAtomicMutableData;
import reka.core.data.ObjBuilder;
import reka.core.data.memory.DataProvider;
import reka.core.data.memory.DataWrapper;
import reka.gitfordata.tree.ObjectId;
import reka.gitfordata.tree.Record;
import reka.gitfordata.tree.RecordReader;
import reka.gitfordata.tree.record.CollectionRecord;
import reka.gitfordata.tree.record.CollectionRecord.Type;
import reka.gitfordata.tree.record.ContentRecord;

public class HashedDataProvider implements DataProvider<Record> {
	
	private final RecordReader reader;
	
	public static VersionedAtomicMutableData create(DataRepoBranch branch, SHA1DataVersion version, Record record) {
		return new HashedDataWrapper(branch, new HashedDataProvider(branch.reader()), version, record);
	}
	
	public static VersionedAtomicMutableData createInitial(DataRepoBranch branch) {
		return create(branch, SHA1DataVersion.initial(), null);
	}
	
	private HashedDataProvider(RecordReader reader) {
		this.reader = reader;
	}

	@Override
	public int sizeOf(Record record) {
		return record.children().size();
	}

	@Override
	public Set<PathElement> elementsOf(Record record) {
		return record.children().keySet();
	}

	@Override
	public Collection<Record> valuesOf(Record record) {
		return record.children().values().stream().map(reader::getRecord).collect(toList());
	}

	@Override
	public Iterator<Entry<PathElement, Data>> iterate(Record record) {
		List<Entry<PathElement, Data>> list = record.children().entrySet().stream().map(this::asEntry).collect(toList());
		return list.iterator();
	}
	
	private Entry<PathElement,Data> asEntry(Entry<PathElement,ObjectId> e) {
		return createEntry(e.getKey(), new DataWrapper<>(reader.getRecord(e.getValue()), this));
	}

	@Override
	public void visitEntries(Record record, BiConsumer<PathElement, Record> visitor) {
		for (Entry<PathElement, ObjectId> e : record.children().entrySet()) {
			visitor.accept(e.getKey(), reader.getRecord(e.getValue()));
		}
	}	
	
	@Override
	public void visitContent(Record record, Path path, BiConsumer<Path, Content> visitor) {
		if (record instanceof ContentRecord) {
			visitor.accept(path, ((ContentRecord) record).toContent());
		} else {
			for (Entry<PathElement, ObjectId> e : record.children().entrySet()) {
				visitContent(reader.getRecord(e.getValue()), path.add(e.getKey()), visitor);
			}
		}
	}

	@Override
	public Record get(Record record, Path path) {
		if (record == null) return null;
		for (PathElement e : path) {
			ObjectId c = record.children().get(e);
			if (c == null) return null;
			record = reader.getRecord(c);
		}
		return record;
	}

	@Override
	public void writeObj(Record record, ObjBuilder builder) {
		if (record instanceof ContentRecord) {
			builder.writeValue(((ContentRecord) record).toContent().value());
		} else if (record instanceof CollectionRecord) {
			CollectionRecord coll = (CollectionRecord) record;
			switch (coll.collectionType()) {
			case MAP:
				builder.writeStartMap();
				for (Entry<PathElement, ObjectId> e : coll.children().entrySet()) {
					builder.writeFieldName(e.getKey().name());
					writeObj(reader.getRecord(e.getValue()), builder);
				}
				builder.writeEndMap();
				break;
			case LIST:
				builder.writeStartList();
				for (ObjectId id : coll.children().values()) {
					writeObj(reader.getRecord(id), builder);
				}
				builder.writeEndList();
				break;
			}
		}
	}

	@Override
	public void out(Record record, JsonGenerator json) throws IOException {
		if (record instanceof ContentRecord) {
			((ContentRecord) record).toContent().out(json);
		} else if (record instanceof CollectionRecord) {
			CollectionRecord coll = (CollectionRecord) record;
			switch (coll.collectionType()) {
			case MAP:
				json.writeStartObject();
				for (Entry<PathElement, ObjectId> e : coll.children().entrySet()) {
					json.writeFieldName(e.getKey().name());
					out(reader.getRecord(e.getValue()), json);
				}
				json.writeEndObject();
				break;
			case LIST:
				json.writeStartArray();
				for (ObjectId id : coll.children().values()) {
					out(reader.getRecord(id), json);
				}
				json.writeEndArray();
				break;
			}
		}
	}

	@Override
	public Record createEmpty() {
		throw unsupported();
	}

	@Override
	public boolean isPresent(Record record) {
		return record != null;
	}

	@Override
	public boolean isMap(Record record) {
		return record instanceof CollectionRecord && ((CollectionRecord) record).collectionType().equals(Type.MAP);
	}

	@Override
	public boolean isList(Record record) {
		return record instanceof CollectionRecord && ((CollectionRecord) record).collectionType().equals(Type.LIST);
	}

	@Override
	public boolean isContent(Record record) {
		return record instanceof ContentRecord;
	}

	@Override
	public Record copy(Record record) {
		return record; // ??
	}

	@Override
	public Content content(Record record) {
		if (!isContent(record)) return null;
		return ((ContentRecord) record).toContent();
	}

}
