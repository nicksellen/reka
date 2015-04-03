package reka.data.storage;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MemoryStorage implements DataStorage {
	
	private final Map<ByteBuffer,ByteBuffer> store;

	public MemoryStorage() {
		this.store = new HashMap<>();
	}
	
	@Override
	public byte[] get(byte[] id) {
		ByteBuffer bb = store.get(ByteBuffer.wrap(id));
		return bb == null ? null : bb.array();
	}

	@Override
	public void close() throws IOException { /* no-op */ }

	@Override
	public DataStorageCommit createCommit() {
		return new MemoryStorageCommit();
	}
	
	private class MemoryStorageCommit implements DataStorageCommit {
		
		private final Map<ByteBuffer,ByteBuffer> added = new HashMap<ByteBuffer,ByteBuffer>();
		private final List<ByteBuffer> removed = new ArrayList<ByteBuffer>();
		
		@Override
		public DataStorageCommit add(byte[] id, byte[] bytes) {
			ByteBuffer idbb = ByteBuffer.wrap(id);
			added.put(idbb, ByteBuffer.wrap(bytes));
			removed.remove(idbb);
			return this;
		}

		@Override
		public DataStorageCommit remove(byte[] id) {
			ByteBuffer idbb = ByteBuffer.wrap(id);
			removed.add(idbb);
			added.remove(idbb);
			return this;
		}

		@Override
		public boolean commit() {
			for (ByteBuffer id : removed) {
				store.remove(id);
			}
			store.putAll(added);
			return true;
		}

		@Override
		public int added() {
			return added.size();
		}

		@Override
		public int removed() {
			return removed.size();
		}
	}

}
