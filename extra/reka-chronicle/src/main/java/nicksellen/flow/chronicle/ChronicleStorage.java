package nicksellen.flow.chronicle;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.api.data.storage.DataStorage;
import reka.api.data.storage.DataStorageCommit;
import reka.util.Util;

import com.higherfrequencytrading.chronicle.Excerpt;
import com.higherfrequencytrading.chronicle.impl.IndexedChronicle;

public class ChronicleStorage implements DataStorage {
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	private final IndexedChronicle chronicle;
	private final Excerpt writeExcerpt, readExcerpt;
	private final Map<ByteHolder, Long> idToIndex = new HashMap<>();
	private final Object lock = new Object();
	
	private static class ByteHolder {
		private final byte[] bytes;
		ByteHolder(byte[] bytes) {
			this.bytes = bytes;
		}
		@Override
		public int hashCode() {
			return Arrays.hashCode(bytes);
		}
		@Override
		public boolean equals(Object object) {
			if (!(object instanceof ByteHolder)) {
				return false;
			}
			ByteHolder other = (ByteHolder) object;
			if (bytes.length != other.bytes.length) {
				return false;
			} else {
				for (int i = 0; i < bytes.length; i++) {
					if (bytes[i] != other.bytes[i]) {
						return false;
					}
				}
			}
			return true;
		}
	}
	
	public ChronicleStorage(String path) {
		try {
			chronicle = new IndexedChronicle(path);
		} catch (IOException e) {
			throw Util.unchecked(e);
		}
		writeExcerpt = chronicle.createExcerpt();
		readExcerpt = chronicle.createExcerpt();
		
		synchronized (lock) {
			while (readExcerpt.nextIndex()) {
				byte[] id = new byte[readExcerpt.readInt()];
				readExcerpt.read(id);
				idToIndex.put(new ByteHolder(id), readExcerpt.index());
			}
			log.debug("loaded %d values\n", idToIndex.size());
		}
	}

	@Override
	public byte[] get(byte[] id) {
		synchronized (lock) {
			Long val = idToIndex.get(new ByteHolder(id));
			if (val != null) {
				if (readExcerpt.index(val)) {
					readExcerpt.skip(readExcerpt.readInt());
					byte[] data = new byte[readExcerpt.readInt()];
					readExcerpt.read(data);
					readExcerpt.finish();
					return data;
				} else {
					throw Util.runtime("was expecting to find an object at index %d", val);
				}
			} else {
				return null;
			}
		}
	}

	@Override
	public void close() throws IOException {
		chronicle.close();
	}

	@Override
	public DataStorageCommit createCommit() {
		return new ChronicleStorageCommit();
	}
	
	private class ChronicleStorageCommit implements DataStorageCommit {

		private final Map<ByteHolder, ByteHolder> added = new HashMap<>();
		private final Set<ByteHolder> removed = new HashSet<>();
		
		@Override
		public DataStorageCommit add(byte[] id, byte[] bytes) {
			ByteHolder idBytes = new ByteHolder(id);
			added.put(idBytes, new ByteHolder(bytes));
			removed.remove(idBytes);
			return this;
		}

		@Override
		public DataStorageCommit remove(byte[] id) {
			throw Util.unsupported();
		}

		@Override
		public boolean commit() {
			
			synchronized (lock) {

				for (Entry<ByteHolder, ByteHolder> entry : added.entrySet()) {
					ByteHolder id = entry.getKey();
					ByteHolder value = entry.getValue();
					writeExcerpt.startExcerpt(4 + 4 + id.bytes.length + value.bytes.length);
					writeExcerpt.writeInt(id.bytes.length);
					writeExcerpt.write(id.bytes);
					writeExcerpt.writeInt(value.bytes.length);
					writeExcerpt.write(value.bytes);
					idToIndex.put(id, writeExcerpt.index());
					writeExcerpt.finish();
				}
			
			}
			return true;
		}

		@Override
		public int added() {
			throw Util.unsupported();
		}

		@Override
		public int removed() {
			throw Util.unsupported();
		}
		
	}

}
