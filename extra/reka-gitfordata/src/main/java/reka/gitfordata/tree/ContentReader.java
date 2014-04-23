package reka.gitfordata.tree;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.api.Path;
import reka.api.Path.PathElement;
import reka.api.content.Content;
import reka.gitfordata.tree.record.CollectionRecord;
import reka.gitfordata.tree.record.CommitRecord;
import reka.gitfordata.tree.record.ContentRecord;

public class ContentReader {
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	private final CollectionRecord root;
	private final RecordReader reader;
	
	public static ContentReader withCommit(RecordReader reader, CommitRecord commit) {
		return new ContentReader(reader, commit);
	}
	
	private ContentReader(RecordReader reader, CommitRecord commit) {
		this.reader = reader;
		this.root = reader.get(commit.tree(), CollectionRecord.class).record();
	}
	
	public Content get(Path path) {
		ObjectIdAndRecord<? extends Record> record = getObjectIdAndRecord(path, root);
		if (record != null && (record.record() instanceof ContentRecord)) {
			return ((ContentRecord) record.record()).toContent();
		} else {
			return null;
		}
	}
	
	public ObjectId getObjectId(Path path) {
		ObjectIdAndRecord<? extends Record> record = getObjectIdAndRecord(path, root);
		if (record != null && (record.record() instanceof ContentRecord)) {
			return record.objectId();
		} else {
			return null;
		}
	}
	
	private ObjectIdAndRecord<? extends Record> getObjectIdAndRecord(Path path, CollectionRecord tree) {

		CollectionRecord currentTree = tree;
		
		for (int i = 0; i < path.length(); i++) {
			PathElement element = path.get(i);
			
			ObjectId item = currentTree.children().get(element);
			
			if (item == null) {
				log.debug("null :( - was looking for [{}] but there was only {}\n", element, currentTree.children().keySet());
				return null;
				
			} else if (i < path.length() - 1) {
				
				// not at the end of the path yet
				
				Record record = reader.get(item).record();
				if (record instanceof CollectionRecord) {
					// we only care if it's a tree
					currentTree = (CollectionRecord) record;
				} else {
					return null;
				}
				
			} else {

				// at the end of the path now
				return reader.get(item);
			}
		}
		
		return null;
	}
}
