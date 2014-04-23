package reka.gitfordata.tree;

import static reka.util.Util.unchecked;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

import com.google.common.io.NullOutputStream;

@SuppressWarnings("deprecation")
public class ObjectIdAndRecord<T extends Record> {
	
	private final ObjectId id;
	private final T record;
	
	@SuppressWarnings("unchecked")
	public static <T extends Record> ObjectIdAndRecord<T> from(ObjectId id, T record) {
		if (record instanceof ObjectIdAndRecord<?>) {
			return (ObjectIdAndRecord<T>) record;
		} else {
			return new ObjectIdAndRecord<T>(id, record);
		}
	}
	
	@SuppressWarnings("unchecked")
	public static <T extends Record> ObjectIdAndRecord<T> from(T record) {
		if (record instanceof ObjectIdAndRecord<?>) {
			return (ObjectIdAndRecord<T>) record;
		}
		OutputStream os = new NullOutputStream();
		try {
			DigestOutputStream digest = new DigestOutputStream(os, MessageDigest.getInstance("sha-1"));
			DataOutputStream dos = new DataOutputStream(digest);
			record.out(dos);
			dos.close();
			return new ObjectIdAndRecord<T>(ObjectId.fromBytes(digest.getMessageDigest().digest()), record);
		} catch (NoSuchAlgorithmException | IOException e) {
			throw unchecked(e);
		}
	}
	
	private ObjectIdAndRecord(ObjectId id, T record) {
		this.id = id;
		this.record = record;
	}

	public ObjectId objectId() {
		return id;
	}

	public T record() {
		return record;
	}
	
	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		} else if (!(object instanceof ObjectIdAndRecord<?>)) {
			return false;
		} else {
			ObjectIdAndRecord<?> other = (ObjectIdAndRecord<?>) object;
			return other.objectId().equals(objectId());
		}
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

}
