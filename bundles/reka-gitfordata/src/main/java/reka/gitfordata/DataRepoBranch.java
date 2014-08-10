package reka.gitfordata;

import static reka.util.Util.unchecked;

import java.io.ByteArrayOutputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.api.Path;
import reka.api.content.Content;
import reka.api.data.MutableData;
import reka.api.data.storage.DataStorage;
import reka.api.data.storage.DataStorageCommit;
import reka.api.data.versioned.VersionedAtomicMutableData;
import reka.api.data.versioned.VersionedData.DataVersion;
import reka.gitfordata.tree.CommitCollector;
import reka.gitfordata.tree.CommitCollector.CommitCollection;
import reka.gitfordata.tree.ObjectId;
import reka.gitfordata.tree.Record;
import reka.gitfordata.tree.RecordReader;
import reka.gitfordata.tree.record.CollectionRecord;
import reka.gitfordata.tree.record.CommitRecord;
import reka.gitfordata.tree.record.ContentRecord;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

public class DataRepoBranch {
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	private final DataStorage meta;
	private final DataStorage objects;
	private final RecordReader reader;
	private final Path location;
	
	DataRepoBranch(DataRepo repo, Path location) {
		this.meta = repo.meta;
		this.objects = repo.objects;
		this.reader = repo.reader;
		this.location = location;
	}
	
	public Path location() {
		return location;
	}

	public VersionedAtomicMutableData atVersion(DataVersion version) {
		ObjectId commitId = ObjectId.fromBytes(version.id());
		CollectionRecord tree = reader.get(reader.getRecord(commitId, CommitRecord.class).tree(), CollectionRecord.class).record();
		return HashedDataProvider.create(this, new SHA1DataVersion(commitId), tree);
	}
	
	protected RecordReader reader() {
		return reader;
	}
	
	public VersionedAtomicMutableData latest() {
		byte[] ref = location.toByteArray();
		byte[] refVal = meta.get(ref);
		if (refVal != null) {
			ObjectId commitId = ObjectId.fromBytes(refVal);
			CollectionRecord tree = reader.get(reader.getRecord(commitId, CommitRecord.class).tree(), CollectionRecord.class).record();
			return HashedDataProvider.create(this, new SHA1DataVersion(commitId), tree);
		} else {
			return HashedDataProvider.createInitial(this);
		}
	}
	
	protected ListenableFuture<SHA1DataVersion> commit(SHA1DataVersion previousVersion, MutableData added, List<Path> removed) {
		
		CommitCollector collector;

		if (previousVersion.equals(SHA1DataVersion.initial())) {
			collector = CommitCollector.newBuilder(reader, "nick", "erm");
		} else {
			collector = CommitCollector.fromPreviousCommit(reader, previousVersion.commitId(), "nick", "erm");
		}
		
		for (Path path : removed) {
			collector.remove(path);
		}
		
		added.forEachContent((path, content) -> {
			collector.add(path, content);
		});			
		
		CommitCollection collection = collector.build();

		DataStorageCommit commit = objects.createCommit();

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutput out = new DataOutputStream(baos);
		
		for (Entry<ObjectId,Record> entry : collection) {
			
			ObjectId objectId = entry.getKey();
			Record record = entry.getValue();
			
			try {
				log.debug("writing {} record", record.getClass());
				if (record instanceof ContentRecord) {
					ContentRecord cr = (ContentRecord) record;
					Content content = cr.toContent();
					log.debug("  with content: {} ({})", content, content.getClass());
				}
				out.writeByte(record.recordType().identifier());
				record.out(out);
				
				commit.add(objectId.bytes(), baos.toByteArray());
				baos.reset();
				
			} catch (IOException e) {
				throw unchecked(e);
			}
		}
		
		commit.commit();
		byte[] ref = location.toByteArray();
		meta.createCommit().add(ref, collection.commitId().bytes()).commit();
		return Futures.immediateFuture(new SHA1DataVersion(collection.commitId()));
	}

}
