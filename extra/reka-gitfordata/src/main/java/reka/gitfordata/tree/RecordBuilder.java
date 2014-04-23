package reka.gitfordata.tree;


import reka.gitfordata.tree.record.builders.CollectionRecordBuilder;

public interface RecordBuilder <T extends Record> {
	
	public T build(RecordCollector collector);
	
	public static interface RecordCollector {
		public void collect(ObjectId id, Record record);
	}
	
	public static class BuilderOrObjectId {
		private final RecordBuilder<?> builder;
		private final ObjectId objectId;
		public BuilderOrObjectId(RecordBuilder<?> builder) {
			this.builder = builder;
			this.objectId = null;
		}
		public BuilderOrObjectId(ObjectId objectId) {
			this.builder = null;
			this.objectId = objectId;
		}
		boolean isBuilder() {
			return builder != null;
		}
		public boolean isEmptyTreeBuilder() {
			return isBuilder() && (builder instanceof CollectionRecordBuilder) && ((CollectionRecordBuilder) builder).isEmpty();
		}
		public boolean isObjectId() {
			return objectId != null;
		}
		public ObjectId objectId() {
			return objectId;
		}
		public RecordBuilder<?> builder() {
			return builder;
		}			
	}
}
