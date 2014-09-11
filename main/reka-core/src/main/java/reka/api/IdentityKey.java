package reka.api;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public final class IdentityKey<T> {
	
	private static final AtomicLong ids = new AtomicLong();
	
	private final long id;
	private final String name;
	
	public static <T> IdentityKey<T> named(String name) {
		return new IdentityKey<>(name);
	}
	
	private IdentityKey(String name) {
		id = ids.incrementAndGet();
		this.name = name;
	}
	
	public long id() {
		return id;
	}
	
	public String name() {
		return name;
	}
	
	public static void main(String[] args) {
		IdentityStore store = IdentityStore.createConcurrentIdentityStore();
		
		IdentityKey<UUID> key = IdentityKey.named("name");
		store.put(key, UUID.randomUUID());
		UUID value = store.get(key);
		
		System.out.printf("uuid is %s\n", value.toString());
		
	}
	
}
