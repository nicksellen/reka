package reka.gitfordata.tree;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import reka.api.Path;
import reka.api.Path.PathElement;
import reka.api.content.Content;
import reka.gitfordata.tree.record.CollectionRecord;
import reka.gitfordata.tree.record.CommitRecord;
import reka.gitfordata.tree.record.ContentRecord;
import reka.gitfordata.tree.record.builders.CollectionRecordBuilder;
import reka.gitfordata.tree.record.builders.CommitRecordBuilder;
import reka.gitfordata.tree.record.builders.ContentRecordBuilder;

public class CommitCollector {

	private final CollectionRecordBuilder rootCollection;
	
	private CommitRecordBuilder commitBuilder;
	
	private RecordReader reader;
	
	public static CommitCollector newBuilder(RecordReader reader, String author, String message) {
		CommitCollector builder = new CommitCollector();
		builder.reader = reader;
		builder.commitBuilder = new CommitRecordBuilder().author(author).message(message);
		return builder;
	}
	
	public static CommitCollector fromPreviousCommit(RecordReader reader, ObjectId commitId, String author, String message) {

		ObjectIdAndRecord<CommitRecord> commit = reader.get(commitId, CommitRecord.class);
		
		CommitCollector builder = new CommitCollector();
		builder.reader = reader;
		builder.commitBuilder = new CommitRecordBuilder().addParent(commitId).author(author).message(message);
		
		ObjectIdAndRecord<CollectionRecord> previousTree = reader.get(commit.record().tree(), CollectionRecord.class);
		
		for (Entry<PathElement, ObjectId> item : previousTree.record().children().entrySet()) {
			// copy the tree references in (but don't actually go and get the objects)
			builder.rootCollection.add(item.getKey(), item.getValue()); 
		}
		
		return builder;
	}
	
	private CommitCollector() {
		rootCollection = new CollectionRecordBuilder();
	}
	
	public CommitCollector committer(String value) {
		commitBuilder.committer(value);
		return this;
	}
	
	public CommitCollector add(Path path, Content content) {
		checkArgument(path.length() > 0, "path must not be empty %s", path);
		return modify(path, Operation.ADD, new ContentRecordBuilder(content));
	}
	
	public CommitCollector remove(Path path) {
		checkArgument(path.length() > 0,"path must not be empty");
		return modify(path, Operation.REMOVE, null);
	}
	
	private static final class DefaultRecordCollector implements RecordBuilder.RecordCollector {
		
		final Map<ObjectId, Record> collected = new HashMap<>();

		@Override
		public void collect(ObjectId id, Record record) {
			collected.put(id, record);
		}
		
	}

	private enum Operation {
		ADD, REMOVE
	}
	
	private CommitCollector modify(Path path, Operation operation, ContentRecordBuilder content) {
		
		checkArgument(operation.equals(Operation.REMOVE) || content != null, "must pass in item to add");
		
		CollectionRecordBuilder currentBuilder = rootCollection;
		RecordBuilder.BuilderOrObjectId existingTreeItem;
		
		List<PathElement> currentPath = new ArrayList<PathElement>();
		
		for (int i = 0; i < path.length(); i++) {
			PathElement element = path.get(i);
			currentPath.add(element);
			
			existingTreeItem = currentBuilder.get(element);
						
			if (i < path.length() - 1) {
				
				if (existingTreeItem == null) {
					
					// no existing item
					
					if (operation.equals(Operation.ADD)) {
						
						// we're not at the end of our path yet so create a new builder
						
						CollectionRecordBuilder builder = new CollectionRecordBuilder();
						currentBuilder.add(element, builder);
						currentBuilder = builder;
						continue;
					
					} else {

						// trying to remove a node where the parent doesn't even exist
						
						return null;
						
					}
					
				} else {
					
					// there is already something in the tree
					
					if (existingTreeItem.isObjectId()) {
						
						// we have a reference to an existing record
						
						ObjectIdAndRecord<?> identifiable = reader.get(existingTreeItem.objectId());
						
						if (identifiable.record() instanceof CollectionRecord) {
							
							// a tree, we need to convert it back into a builder so
							// we can add more things to it later
							
							CollectionRecordBuilder builder = ((CollectionRecord) identifiable.record()).toBuilder();
							currentBuilder.add(element, builder);
							currentBuilder =  builder;
							
							continue;
							
						} else if (identifiable.record() instanceof ContentRecord) {
							
							if (operation.equals(Operation.ADD)) {
								
								// we're drilling down our path but found a casualty
								// sorry, this content is history!
								
								CollectionRecordBuilder newBuilder = new CollectionRecordBuilder();
								currentBuilder.add(element, newBuilder);
								currentBuilder = newBuilder;
								
								continue;
								
							} else {
								
								// if it's not a tree and we're not at the end of
								// the path and we're removing, there cannot
								// be an item where we've said so we can safely bail
								
								return this;
							}
							
						}
					
					} else if (existingTreeItem.isBuilder()) {
						
						// already is a builder
						
						RecordBuilder<?> builder = existingTreeItem.builder();
						
						if (builder instanceof CollectionRecordBuilder) {
							
							currentBuilder = (CollectionRecordBuilder) builder;

							continue;
							
						} else {
							
							if (operation.equals(Operation.ADD)) {
								
								CollectionRecordBuilder newBuilder = new CollectionRecordBuilder();
								currentBuilder.add(element, newBuilder);
								currentBuilder = newBuilder;
								
								continue;
								
							} else {
								
								return this;
							}
							
						}
					}
					
				}
			
			} else {

				// we're at the specified place now (the end of our path)
				
				if (existingTreeItem != null && existingTreeItem.isObjectId()) {

					// whether we're doing ADD or REMOVE we need to
					// get rid of the current imposter
					
					// we only care if we're removing a real thing
				
					currentBuilder.remove(element);
				}
				
				if (operation.equals(Operation.ADD)) {
					currentBuilder.add(element, content); // TODO: I removed the .build() from here, hope it's ok
				}
			}
		}
		
		return this;
	}
	
	public static class CommitCollection implements Iterable<Entry<ObjectId,Record>> {
		
		private final ObjectId commitId;
		private final Map<ObjectId,Record> collected;
		
		CommitCollection(ObjectId commitId, Map<ObjectId,Record> collected) {
			this.commitId = commitId;
			this.collected = collected;
		}
		
		public ObjectId commitId() {
			return commitId;
		}

		@Override
		public Iterator<Entry<ObjectId, Record>> iterator() {
			return collected.entrySet().iterator();
		}
	}
	
	public CommitCollection build() {
		DefaultRecordCollector collector = new DefaultRecordCollector();
		
		ObjectIdAndRecord<CollectionRecord> tree = ObjectIdAndRecord.from(rootCollection.build(collector));
		ObjectIdAndRecord<CommitRecord> commit = ObjectIdAndRecord.from(commitBuilder.tree(tree.objectId()).build(collector));
		
		collector.collected.put(tree.objectId(), tree.record());
		collector.collected.put(commit.objectId(), commit.record());
		
		return new CommitCollection(commit.objectId(), collector.collected);
	}
	
}
