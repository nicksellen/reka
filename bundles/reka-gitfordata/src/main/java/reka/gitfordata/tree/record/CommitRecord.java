package reka.gitfordata.tree.record;

import static reka.util.Util.unchecked;

import java.io.DataOutput;
import java.io.IOException;
import java.util.Set;

import reka.gitfordata.tree.ObjectId;
import reka.gitfordata.tree.Record;
import reka.gitfordata.tree.record.builders.CommitRecordBuilder;

public final class CommitRecord implements Record {

	private final Set<ObjectId> parentIds;
	private final ObjectId tree;
	private final String committer;
	private final String author;
	private final String message;

	public static CommitRecordBuilder newBuilder(ObjectId tree, String author, String message) {
		return new CommitRecordBuilder(tree, author, message);
	}
	
	public static CommitRecordBuilder newBuilder() {
		return new CommitRecordBuilder();
	}

	public CommitRecord(Set<ObjectId> parentIds, ObjectId tree, String comitter, String author, String message) {
		this.parentIds = parentIds;
		this.tree = tree;
		this.committer = comitter;
		this.author = author;
		this.message = message;
	}

	public Set<ObjectId> parentIds() {
		return parentIds;
	}

	public String committer() {
		return committer;
	}

	public String author() {
		return author;
	}

	public String message() {
		return message;
	}

	public ObjectId tree() {
		return tree;
	}
	
	@Override
	public void out(DataOutput out) {
		
		try {
			out.writeInt(parentIds.size());
			for (ObjectId parentId : parentIds) {
				out.write(parentId.bytes());
			}
			out.write(tree.bytes());
			
			if (author != null) {
				out.writeBoolean(true);
				out.writeUTF(author);
			} else {
				out.writeBoolean(false);
			}
			
			if (committer != null) {
				out.writeBoolean(true);
				out.writeUTF(committer);
			} else {
				out.writeBoolean(false);
			}
			
			if (message != null) {
				out.writeBoolean(true);
				out.writeUTF(message);
			} else {
				out.writeBoolean(false);
			}
			
		} catch (IOException e) {
			throw unchecked(e);
		}
	}

	@Override
	public Type recordType() {
		return Type.COMMIT;
	}

}
