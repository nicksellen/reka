package reka.leveldb;

import static java.lang.String.format;
import static org.fusesource.leveldbjni.JniDBFactory.factory;
import static reka.util.Util.unchecked;

import java.io.File;
import java.io.IOException;

import org.iq80.leveldb.CompressionType;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBIterator;
import org.iq80.leveldb.Options;
import org.iq80.leveldb.WriteBatch;
import org.iq80.leveldb.WriteOptions;

import reka.api.data.storage.DataStorage;
import reka.api.data.storage.DataStorageCommit;
import reka.util.Util;

public class LevelDBStorage implements DataStorage {

	private String dbfile;
	private DB db;
	
	private static final int OneMB = 1024 * 1024;
	private static final int OneKB = 1024;
	private final Options options;
	
	private static final WriteOptions WRITE_OPTIONS = new WriteOptions().sync(false);
	
	public LevelDBStorage(String dbfile) {
		this(dbfile, 0, 64 * OneKB, 2 * OneMB);
	}
	
	public LevelDBStorage(String dbfile, long cacheSize, int blockSize, int writeBuffer) {
		this.dbfile = dbfile;
		options = new Options().createIfMissing(true)
				.cacheSize(cacheSize)
				.blockSize(blockSize)
				.writeBufferSize(writeBuffer)
				.compressionType(CompressionType.SNAPPY);
		db(); // ensure we create the db now
	}
	
	@Override
	public String toString() {
		return format("<LevelDB path=%s>",dbfile);
	}

	private DB db() {
		if (db == null) {
			try {
				db = factory.open(new File(dbfile), options);
			} catch (IOException e) {
				throw Util.unchecked(e);
			}
		}
		return db;
	}
	
	
	@Override
	public DataStorageCommit createCommit() {
		return new LevelDBCommit();
	}

	@Override
	public byte[] get(byte[] id) {
		return db().get(id);
	}
	
	public DBIterator iterator() {
		return db().iterator();
	}

	@Override
	public void close() {
		
		if (db != null) {
			try {
				db.close();
				db = null;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
	}
	
	private class LevelDBCommit implements DataStorageCommit {

		private final WriteBatch batch;

		private int added = 0;
		private int removed = 0;
		
		private LevelDBCommit() {
			batch = db().createWriteBatch();
		}
		
		@Override
		public DataStorageCommit add(byte[] id, byte[] bytes) {
			batch.put(id, bytes);
			added++;
			return this;
		}

		@Override
		public DataStorageCommit remove(byte[] id) {
			batch.delete(id);
			removed++;
			return this;
		}
		
		@Override
		public boolean commit() {
			db.write(batch, WRITE_OPTIONS);
			try {
				batch.close();
				return true;
			} catch (IOException e) {
				throw unchecked(e);
			}
		}

		@Override
		public int added() {
			return added;
		}

		@Override
		public int removed() {
			return removed;
		}

	}

}
