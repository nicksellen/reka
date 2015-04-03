package reka.data;

import static java.util.stream.Collectors.toSet;
import static reka.util.Util.createEntry;

import java.util.AbstractMap;
import java.util.Set;

import reka.util.Path.PathElement;

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
	
	@Override
	public String toString() {
		return data.toJson();
	}
	
}
