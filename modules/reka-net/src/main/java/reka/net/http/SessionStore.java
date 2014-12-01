package reka.net.http;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import reka.api.data.MutableData;
import reka.core.data.memory.MutableMemoryData;

public class SessionStore {
	
	private final ConcurrentMap<String,MutableData> store = new ConcurrentHashMap<>();
	
	public MutableData findOrCreate(String sessionid) {
		return store.computeIfAbsent(sessionid, (unused) -> MutableMemoryData.create());
	}
	
	public Optional<MutableData> find(String sessionid) {
		return Optional.ofNullable(store.get(sessionid));
	}
	
	public void remove(String sessionid) {
		store.remove(sessionid);
	}

}
