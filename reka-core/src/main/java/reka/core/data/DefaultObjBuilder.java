package reka.core.data;

import static com.google.common.base.Preconditions.checkState;
import static reka.util.Util.unchecked;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.map.ObjectMapper;

public class DefaultObjBuilder implements ObjBuilder {

	private static final ObjectMapper mapper = new ObjectMapper();
	
	private final Deque<Object> stack = new ArrayDeque<>();
	private String fieldName = null;
	private Object last;
	
	@Override
	public void writeStartMap() {
		Map<String,Object> map = new JsonLinkedHashMap<>();
		if (!stack.isEmpty()) {
			writeValue(map);
		}
		stack.push(map);
	}
	
	@Override
	public void writeEndMap() {
		last = stack.pop();
		checkState(last instanceof Map, "expected to pop a map");
	}
	
	@Override
	public void writeStartList() {
		List<Object> list = new ArrayList<>(); // TODO: fixup JsonList...
		if (!stack.isEmpty()) {
			writeValue(list);
		}
		stack.push(list);
	}
	
	@Override
	public void writeEndList() {
		last = stack.pop();
		checkState(last instanceof List, "expected to pop a list");
	}
	
	@Override
	public void writeFieldName(String name) {
		checkState(stack.peek() instanceof Map, "only maps can have a field name");
		fieldName = name;
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public void writeValue(Object value) {
		if (fieldName != null) {
			((Map<String,Object>) stack.peek()).put(fieldName, value);
			fieldName = null;
		} else {
			((List<Object>) stack.peek()).add(value);
		}
	}
	
	@Override
	public Object obj() {
		return last;
	}
	
	private static class JsonLinkedHashMap<K,V> extends LinkedHashMap<K,V> {
		
		private static final long serialVersionUID = -3424826044505645508L;
		
		JsonLinkedHashMap() {
			super();
		}
		
		@Override
		public String toString() {
			try {
				return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(this);
			} catch (IOException e) {
				throw unchecked(e);
			}
		}
		
	}
	
}