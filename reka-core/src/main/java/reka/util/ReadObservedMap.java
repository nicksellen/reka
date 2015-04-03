package reka.util;

import static java.lang.String.format;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import reka.api.Path;

public class ReadObservedMap<K,V> implements Map<K,V> {
	
	public static <K,V> ReadObservedMap<K,V> wrap(Map<K,V> inner) {
		return new ReadObservedMap<>(inner, Path.empty(),
			new TreeSet<Path>(), 
			new TreeSet<Path>());
	}
	
	private final Map<K,V> inner;
	private final Path path;
	private final Set<Path> valuePaths;
	private final Set<Path> iteratedPaths;
	
	public Collection<Path> valuePaths() {
		return valuePaths;
	}
	
	public Collection<Path> iteratedPaths() {
		return iteratedPaths;
	}
	
	private ReadObservedMap(Map<K,V> map, Path path, Set<Path> valuePaths, Set<Path> iteratedPaths) {
		this.inner = map;
		this.path = path;
		this.valuePaths = valuePaths;
		this.iteratedPaths = iteratedPaths;
	}

	@Override
	public int size() {
		return inner.size();
	}

	@Override
	public boolean isEmpty() {
		return inner.isEmpty();
	}

	@Override
	public boolean containsKey(Object key) {
		return inner.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return inner.containsValue(value);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public V get(Object key) {
		Path valuePath = path.add(key.toString());
		V value = inner.get(key);
		if (value instanceof Map) {
			return (V) new ReadObservedMap((Map) value, valuePath, valuePaths, iteratedPaths);
		} else {
			valuePaths.add(valuePath);
			return value;
		}
	}

	@Override
	public V put(K key, V value) {
		return inner.put(key, value);
	}
	
	@Override
	public String toString() {
		return inner.toString();
	}

	@Override
	public V remove(Object key) {
		return inner.remove(key);
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
		inner.putAll(m);
	}

	@Override
	public void clear() {
		throw new UnsupportedOperationException(format("sorry, you can't clear an %s", getClass().getSimpleName()));
	}

	@Override
	public Set<K> keySet() {
		iteratedPaths.add(path);
		return inner.keySet();
	}

	@Override
	public Collection<V> values() {
		return inner.values();
	}

	@Override
	public Set<java.util.Map.Entry<K, V>> entrySet() {
		iteratedPaths.add(path);
		Set<Map.Entry<K,V>> wrapped = new HashSet<>();
		for (Entry<K,V> entry : inner.entrySet()) {
			wrapped.add(new ObservedEntry<K,V>(entry, path.add(entry.getKey().toString())));
		}
		return wrapped;
	}
	
	private class ObservedEntry<Ke,Ve> implements Map.Entry<Ke,Ve> {
		
		private final Map.Entry<Ke, Ve> inner;
		private final Path path;
		
		ObservedEntry(Map.Entry<Ke, Ve> entry, Path path) {
			this.inner = entry;
			this.path = path;
		}

		@Override
		public Ke getKey() {
			return inner.getKey();
		}

		@SuppressWarnings({ "rawtypes", "unchecked" })
		@Override
		public Ve getValue() {
			Ve value = inner.getValue();
			if (value instanceof Map) {
				return (Ve) new ReadObservedMap((Map) value, path, valuePaths, iteratedPaths);
			} else {
				valuePaths.add(path);
				return value;
			}
		}

		@Override
		public Ve setValue(Ve value) {
			return inner.setValue(value);
		}
		
		@Override
		public String toString() {
			return inner.toString();
		}
		
	}
	
}