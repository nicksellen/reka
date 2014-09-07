package reka.api;

import java.util.UUID;

public final class IdentityKey<T> {

	private final String name;
	
	public static <T> IdentityKey<T> named(String name) {
		return new IdentityKey<>(name);
	}
	
	private IdentityKey(String name) {
		this.name = name;
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
