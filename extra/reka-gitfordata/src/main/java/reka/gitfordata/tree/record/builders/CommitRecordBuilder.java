package reka.gitfordata.tree.record.builders;

import static reka.util.Util.unchecked;

import java.io.DataInput;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import reka.gitfordata.tree.ObjectId;
import reka.gitfordata.tree.RecordBuilder;
import reka.gitfordata.tree.record.CommitRecord;

public class CommitRecordBuilder implements RecordBuilder<CommitRecord> {
	
	public static CommitRecord in(DataInput in) {
		try {
			CommitRecordBuilder builder = new CommitRecordBuilder();
			
			int parentSize = in.readInt();
			
			for (int i = 0; i < parentSize; i++) {
				byte[] bytes = new byte[ObjectId.SIZE];
				in.readFully(bytes);
				builder.addParent(ObjectId.fromBytes(bytes));
			}
			byte[] bytes = new byte[ObjectId.SIZE];
			in.readFully(bytes);
			builder.tree(ObjectId.fromBytes(bytes));
			if (in.readBoolean()) {
				builder.author(in.readUTF());
			}
			if (in.readBoolean()) {
				builder.committer(in.readUTF());
			}
			if (in.readBoolean()) {
				builder.message(in.readUTF());
			}
			return builder.build(null);
		} catch (IOException e) {
			throw unchecked(e);
		}
	}

	private ObjectId tree;
	private String author;
	private String committer;
	private String message;
	
	private Set<ObjectId> parentIds = new HashSet<>();

	public CommitRecordBuilder(ObjectId tree, String author, String message) {
		this.tree = tree;
		this.author = author;
		this.message = message;
	}
	
	public CommitRecordBuilder() {
		
	}

	public CommitRecordBuilder tree(ObjectId value) {
		tree = value;
		return this;
	}

	public CommitRecordBuilder committer(String value) {
		this.committer = value;
		return this;
	}

	public CommitRecordBuilder author(String value) {
		this.author = value;
		return this;
	}

	public CommitRecordBuilder message(String value) {
		this.message = value;
		return this;
	}

	public CommitRecordBuilder addParent(ObjectId objectId) {
		parentIds.add(objectId);
		return this;
	}

	@Override
	public CommitRecord build(RecordCollector collector) {
		return new CommitRecord(parentIds, tree, committer, author, message);


	}
}
