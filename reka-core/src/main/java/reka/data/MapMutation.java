package reka.data;

import static reka.api.Path.path;
import static reka.data.content.Contents.booleanValue;
import static reka.data.content.Contents.integer;
import static reka.data.content.Contents.longValue;
import static reka.data.content.Contents.utf8;
import reka.api.Path;
import reka.data.content.Content;
import reka.util.ThrowingConsumer;

public interface MapMutation {
	
	MapMutation remove(Path key);
	MapMutation put(Path key, Data data);
	MapMutation put(Path key, Content content);
	MapMutation putList(Path key, ThrowingConsumer<ListMutation> list);
	MapMutation putMap(Path key, ThrowingConsumer<MapMutation> map);
	
	default MapMutation putInt(Path key, int val) {
		return put(key, integer(val));
	}
	
	default MapMutation putLong(Path key, long val) {
		return put(key, longValue(val));
	}
	
	default MapMutation putString(Path key, String val) {
		return put(key, utf8(val));
	}
	
	// helper string key versions	
	
	default MapMutation put(String key, Data data) {
		return put(path(key), data);
	}
	
	default MapMutation put(String key, Content content) {
		return put(path(key), content);
	}
	
	default MapMutation putInt(String key, int val) {
		return put(key, integer(val));
	}
	
	default MapMutation putLong(String key, long val) {
		return put(key, longValue(val));
	}
	
	default MapMutation putBool(String key, boolean val) {
		return put(key, booleanValue(val));
	}
	
	default MapMutation putString(String key, String val) {
		return put(key, utf8(val));
	}

	default MapMutation putList(String key, ThrowingConsumer<ListMutation> list) {
		return putList(path(key), list);
	}
	
	default MapMutation putMap(String key, ThrowingConsumer<MapMutation> map) {
		return putMap(path(key), map);
	}
	
	default MapMutation remove(String key) {
		return remove(path(key));
	}
	
}
