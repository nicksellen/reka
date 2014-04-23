package reka.api.data.storage;

import static java.lang.String.format;

import java.io.IOException;

import com.google.common.primitives.Bytes;

public class PrefixedDataStorage implements DataStorage {

	@Override
	public String toString() {
		return format("<PrefixedDataStorage storage=%s>", storage);
	}

	private final byte[] prefix;
	private final DataStorage storage;
	
	public PrefixedDataStorage(byte[] prefix, DataStorage storage) {
		this.prefix = prefix;
		this.storage = storage;
	}
	
	public PrefixedDataStorage(byte prefix, DataStorage storage) {
		this.prefix = new byte[]{prefix};
		this.storage = storage;
	}
	
	public PrefixedDataStorage(int prefix, DataStorage storage) {
		this.prefix = new byte[]{(byte)prefix};
		this.storage = storage;
	}
	
	@Override
	public byte[] get(byte[] key) {
		return storage.get(Bytes.concat(prefix, key));
	}

	@Override
	public void close() throws IOException {
		storage.close();
	}

	@Override
	public DataStorageCommit createCommit() {
		return new PrefixedDataStorageCommit(storage.createCommit());
	}
	
	private class PrefixedDataStorageCommit implements DataStorageCommit {
		
		private final DataStorageCommit commit;
		
		PrefixedDataStorageCommit(DataStorageCommit commit) {
			this.commit = commit;
		}

		@Override
		public DataStorageCommit add(byte[] id, byte[] bytes) {
			return commit.add(Bytes.concat(prefix,id), bytes);
		}

		@Override
		public DataStorageCommit remove(byte[] id) {
			return commit.remove(Bytes.concat(prefix,id));
		}

		@Override
		public boolean commit() {
			return commit.commit();
		}

		@Override
		public int added() {
			return commit.added();
		}

		@Override
		public int removed() {
			return commit.removed();
		}
		
	}

}
