package reka.core.data.memory;

import java.util.List;
import java.util.Map;

import reka.api.Path;
import reka.api.content.Content;

public interface MutableDataProvider<T> extends DataProvider<T> {
	
	T remove(T obj, Path path);
	
	T clear(T obj);
	
	T put(T obj, Path path, T data);
	T put(T obj, Path path, Map<String,T> map);
	T put(T obj, Path path, List<T> list);
	
	T putOrAppend(T obj, Path path, T data);

	T putContent(T obj, Path path, Content content);
	T putOrAppendContent(T obj, Path path, Content content);

	T createMap();
	T createList();
	
}