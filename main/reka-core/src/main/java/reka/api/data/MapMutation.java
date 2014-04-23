package reka.api.data;

import static reka.api.content.Contents.integer;
import static reka.api.content.Contents.longValue;
import static reka.api.content.Contents.utf8;

import java.util.function.Consumer;

import reka.api.content.Content;

public interface MapMutation {
	
	MapMutation remove(String key);
	MapMutation put(String key, Data data);
	MapMutation put(String key, Content content);
	MapMutation putList(String key, Consumer<ListMutation> list);
	MapMutation putMap(String key, Consumer<MapMutation> map);
	
	default MapMutation putInt(String key, int val) {
		return put(key, integer(val));
	}
	
	default MapMutation putLong(String key, long val) {
		return put(key, longValue(val));
	}
	
	default MapMutation putString(String key, String val) {
		return put(key, utf8(val));
	}
}
