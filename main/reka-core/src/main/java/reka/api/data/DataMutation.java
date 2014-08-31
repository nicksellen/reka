package reka.api.data;

import static reka.api.Path.path;
import static reka.api.Path.PathElements.name;
import static reka.api.content.Contents.falseValue;
import static reka.api.content.Contents.integer;
import static reka.api.content.Contents.longValue;
import static reka.api.content.Contents.nullValue;
import static reka.api.content.Contents.trueValue;
import static reka.api.content.Contents.utf8;

import java.util.function.Consumer;

import reka.api.Path;
import reka.api.Path.PathElement;
import reka.api.Path.PathElements;
import reka.api.content.Content;

public interface DataMutation<T> {

	T put(Path path, Content content);
	T put(Path path, Data data);
	T putOrAppend(Path path, Content content);
	T putOrAppend(Path path, Data data);

	T remove(Path path);

	MutableData createMapAt(Path path);
	MutableData createListAt(Path path);
	
	T putMap(Path path, Consumer<MapMutation> map);
	T putList(Path path, Consumer<ListMutation> list);

	default T put(String key, Data data) {
		return put(path(key), data);
	}
	
	default MutableData createMapAt(PathElement element) {
		return createMapAt(path(element));
	}
	
	default MutableData createMapAt(String key) {
		return createMapAt(name(key));
	}
	
	default MutableData createListAt(PathElement element) {
		return createListAt(path(element));
	}
	
	default T putString(Path path, String val) {
		return put(path, utf8(val));
	}
	
	default T putString(String key, String val) {
		return put(key, utf8(val));
	}
	
	default T putInt(Path path, int val) {
		return put(path, integer(val));
	}
	
	default T putInt(String key, int val) {
		return put(key, integer(val));
	}
	
	default T putLong(Path path, long val) {
		return put(path, longValue(val));
	}
	
	default T putLong(PathElement element, long val) {
		return putLong(path(element), val);
	}
	
	default T putLong(String key, long val) {
		return putLong(PathElements.name(key), val);
	}
	
	default T putBool(Path path, boolean val) {
		return put(path, val ? trueValue() : falseValue());
	}
	
	default T putBool(PathElement element, boolean val) {
		return putBool(path(element), val);
	}
	
	default T putBool(String key, boolean val) {
		return putBool(PathElements.name(key), val);
	}

	default T putNull(Path path) {
		return put(path, nullValue());
	}
	
	default T put(String key, Content content) {
		return put(PathElements.name(key), content);
	}
	
	default T putLong(int index, long val) {
		return putLong(PathElements.index(index), val);
	}
	
	default T putBool(int index, boolean val) {
		return putBool(PathElements.index(index), val);
	}

	default T put(int index, Content content) {
		return put(PathElements.index(index), content);
	}
	
	default T put(PathElement element, Content content) {
		return put(path(element), content);
	}
	
	default T remove(PathElement element) {
		return remove(Path.path(element));
	}
	
	default T remove(String key) {
		return remove(PathElements.name(key));
	}
	
	default T remove(int index) {
		return remove(PathElements.index(index));
	}
	
	default T putMap(PathElement element, Consumer<MapMutation> map) {
		return putMap(path(element), map);
	}
	
	default T putMap(String key, Consumer<MapMutation> map) {
		return putMap(PathElements.name(key), map);
	}
	
	default T putMap(int index, Consumer<MapMutation> map) {
		return putMap(PathElements.index(index), map);
	}
	
	default T putList(PathElement element, Consumer<ListMutation> list) {
		return putList(path(element), list);
	}
	
	default T putList(String key, Consumer<ListMutation> list) {
		return putList(PathElements.name(key), list);
	}
	
	default T putList(int index, Consumer<ListMutation> list) {
		return putList(PathElements.index(index), list);
	}

}
