package reka.data.memory;

import static reka.util.Util.unchecked;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.function.BiConsumer;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;

import reka.api.Path;
import reka.api.Path.PathElement;
import reka.data.Data;
import reka.data.ObjBuilder;
import reka.data.content.Content;

public interface DataProvider<T> {
	
	static final JsonFactory factory = new JsonFactory();
	
	default String toPrettyJson(T obj) {
		try {
			StringWriter writer = new StringWriter();
			JsonGenerator json = factory.createJsonGenerator(writer);
			json.useDefaultPrettyPrinter();
			out(obj, json);
			json.flush();
			return writer.toString();
		} catch (IOException e) {
			throw unchecked(e);
		}
	}
	
	default String toJson(T obj) {
		try {
			StringWriter writer = new StringWriter();
			JsonGenerator json = factory.createJsonGenerator(writer);
			out(obj, json);
			json.flush();
			return writer.toString();
		} catch (IOException e) {
			throw unchecked(e);
		}
	}
	
	int sizeOf(T obj);
	
	Collection<PathElement> elementsOf(T obj);
	Collection<T> valuesOf(T obj);
	
	void visitContent(T obj, Path path, BiConsumer<Path, Content> visitor);
	void visitEntries(T obj, BiConsumer<PathElement,T> visitor);
	
	Iterator<Entry<PathElement,Data>> iterate(T obj);
	
	T get(T obj, Path path);
	
	default Content getContent(T obj, Path path) {
		return content(get(obj, path));
	}
	
	void writeObj(T o, ObjBuilder builder);
	void out(T o, JsonGenerator json) throws IOException;
	
	default boolean existsAt(T o, Path path) {
		return get(o, path) != null;
	}
	
	default boolean contentExistsAt(T o, Path path) {
		T val = get(o, path);
		return val != null && isContent(val);
	}
	
	boolean isPresent(T ojb);
	boolean isMap(T obj);
	boolean isList(T obj);
	boolean isContent(T obj);
	
	Content content(T obj);
	
	T copy(T obj);

	T createEmpty();
	
}