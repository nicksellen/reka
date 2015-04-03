package reka.identity;

import static java.lang.System.identityHashCode;

import java.util.Map;
import java.util.Optional;

import com.google.common.collect.ImmutableMap;

public class ImmutableIdentityStore implements IdentityStoreReader {
	
	protected final Map<Integer, Object> map;
	
	public static IdentityStoreReader from(Map<Integer,Object> source) {
		return new ImmutableIdentityStore(ImmutableMap.copyOf(source));
	}
	
	public static class Builder {
		private final ImmutableMap.Builder<Integer,Object> m = ImmutableMap.builder();
		public <T> Builder put(IdentityKey<T> key, T object) {
			m.put(identityHashCode(key),object);
			return this;
		}
		public IdentityStoreReader build() {
			return new ImmutableIdentityStore(m.build());
		}
	}
	
	private ImmutableIdentityStore(Map<Integer,Object> map) {
		this.map = map;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <T> T get(IdentityKey<T> key) {
		return (T) map.get(identityHashCode(key));
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> Optional<T> lookup(IdentityKey<T> key) {
		return Optional.ofNullable((T) map.get(identityHashCode(key)));
	}

	@Override
	public <T> boolean has(IdentityKey<T> key) {
		return map.containsKey(key);
	}
	
}