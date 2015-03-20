package reka.nashorn;

import static reka.util.Util.createEntry;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Sets;

public class NashornDataWrapper {
	
	@SuppressWarnings({ "restriction", "unchecked" })
	public static Object convert(Object value) {
		if (value instanceof Map) {
			
			// this triggers us to convert the whole tree... would be good to avoid
			Map<String,Object> m = (Map<String,Object>) value;
			Map<String,Object> mConverted = new HashMap<>();
			
			m.forEach((k, v) -> {
				mConverted.put(k, convert(v));
			});
			
			return jdk.nashorn.internal.objects.Global.toObject(mConverted);
			
		} else if (value instanceof Collection) {
			Collection<Object> list = (Collection<Object>) value;;
			Iterator<Object> it = list.iterator();
			Object[] array = new Object[list.size()];
			int i = 0;
			while (it.hasNext()) {
				array[i++] = convert(it.next());
			}
			return jdk.nashorn.internal.objects.Global.toObject(array);
		} else {
			return value;
		}
	}
	
	public static Map<String,Object> wrapMap(Map<String,Object> m) {
		return new WrappedMap(m);
	}
	
	public static class WrappedMap extends AbstractMap<String,Object> {

		private final Map<String,Object> source; // readonly, holds original values
		private final Map<String,Object> m;		 // writable, holds converted values

		private Set<Map.Entry<String, Object>> s; // cache of entryset
		
		public WrappedMap(Map<String,Object> source) {
			this.source = source; 
			m = new HashMap<>(source.size()); // our own writable copy
		}

		@Override
		public Set<Map.Entry<String, Object>> entrySet() {
			if (s == null) {
				s = new HashSet<>();
				keySet().forEach(k -> {
					s.add(createEntry(k, get(k)));
				});
			}
			return s;
		}
		
		@Override
		public Object get(Object key) {
			if (m.containsKey(key)) {
				return m.get(key);
			} else if (source.containsKey(key)) {
				Object v = convert(source.get(key));
				m.put((String)key, v);
				return v;
			}
			return null;
		}

		@Override
		public int size() {
			return m.size();
		}

		@Override
		public boolean containsKey(Object key) {
			return m.containsKey(key) || source.containsKey(key);
		}

		@Override
		public Set<String> keySet() {
			return Sets.union(source.keySet(), m.keySet());
		}

		// must only put stuff in here from JS...
		// we assume it is already Native* stuff

		@Override
		public Object put(String key, Object value) {
			s = null; // reset
			return m.put(key, value);
		}

		@Override
		public Object remove(Object key) {
			s = null; // reset
			return m.remove(key); // limitation: can only remove things you added
		}

		@Override
		public void putAll(Map<? extends String, ? extends Object> m2) {
			s = null; // reset
			m.putAll(m2);
		}
		
	}

}
