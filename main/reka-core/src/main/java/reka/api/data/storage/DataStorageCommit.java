package reka.api.data.storage;

public interface DataStorageCommit {
	public DataStorageCommit add(byte[] id, byte[] bytes);
	public DataStorageCommit remove(byte[] id);
	public boolean commit();
	public int added();
	public int removed();
}
