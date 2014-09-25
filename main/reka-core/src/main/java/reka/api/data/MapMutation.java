package reka.api.data;

import static reka.api.Path.path;
import static reka.api.content.Contents.booleanValue;
import static reka.api.content.Contents.integer;
import static reka.api.content.Contents.longValue;
import static reka.api.content.Contents.utf8;

import java.util.function.Consumer;

import reka.api.Path;
import reka.api.content.Content;

public interface MapMutation {
	
	MapMutation remove(Path key);
	MapMutation put(Path key, Data data);
	MapMutation put(Path key, Content content);
	MapMutation putList(Path key, Consumer<ListMutation> list);
	MapMutation putMap(Path key, Consumer<MapMutation> map);
	
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

	default MapMutation putList(String key, Consumer<ListMutation> list) {
		return putList(path(key), list);
	}
	
	default MapMutation putMap(String key, Consumer<MapMutation> map) {
		return putMap(path(key), map);
	}
	
	default MapMutation remove(String key) {
		return remove(path(key));
	}
	
}
