package reka.api.data;

import static java.util.stream.Collectors.toSet;
import static reka.util.Util.createEntry;

import java.util.AbstractMap;
import java.util.Set;

import reka.api.Path.PathElement;

import com.google.common.collect.ImmutableSet;

public class DataMapView extends AbstractMap<String, Object> {

	private final Data data;
	
	private Set<Entry<String, Object>> entrySet;
	
	public DataMapView(Data data) {
		this.data = data;
	}
	
	@Override
	public int size() {
		return data.size();
	}

	@Override
	public Object get(Object key) {
		if (!(key instanceof String)) return false;
		return DataViewUtil.convert(data.at((String) key));
	}

	@Override
	public boolean containsKey(Object key) {
		if (!(key instanceof String)) return false;
		return data.existsAt((String) key);
	}

	@Override
	public Set<String> keySet() {
		return data.elements().stream().map(Object::toString).collect(toSet());
	}

	@Override
	public Set<java.util.Map.Entry<String, Object>> entrySet() {
		if (entrySet == null) {
			ImmutableSet.Builder<Entry<String,Object>> b = ImmutableSet.builder();
			for (PathElement e : data.elements()) {
				b.add(createEntry(e.toString(), DataViewUtil.convert(data.at(e))));
			}
			entrySet = b.build();
		}
		return entrySet;
	}

	
	/*
	 * 
	 * 
	@Override
	public int size() {
		return data.size();
	}

	@Override
	public boolean isEmpty() {
		return data.size() > 0;
	}

	@Override
	public boolean containsKey(Object key) {
		if (!(key instanceof String)) return false;
		return data.existsAt((String) key);
	}

	@Override
	public boolean containsValue(Object value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Object get(Object key) {
		if (!(key instanceof String)) return null;
		Data val = data.at((String) key);
		if (!val.isPresent()) return null;
		
		if (val.isMap()) {
			return new DataMapView(val);
		} else if (val.isList()) {
			return new DataListView(val);
		} else if (val.isContent()) {
			return val.content().value();
		} else {
			return null;
		}
	}

	@Override
	public Object put(String key, Object value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Object remove(Object key) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void putAll(Map<? extends String, ? extends Object> m) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clear() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Set<String> keySet() {
		return data.elements().stream().map(Object::toString).collect(toSet());
	}

	@Override
	public Collection<Object> values() {
		
		return null;
	}

	@Override
	public Set<Entry<String, Object>> entrySet() {
		// TODO Auto-generated method stub
		return null;
	}
	*/

}
