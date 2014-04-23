package reka.api.data;

import static reka.api.Path.path;
import static reka.api.Path.root;
import static reka.api.data.DataUtils.dataIsEqual;
import static reka.api.data.DataUtils.diffContent;
import static reka.api.data.DataUtils.diffPath;
import static reka.api.data.DataUtils.getFirstContent;
import static reka.api.data.DataUtils.visitFirstContent;
import static reka.api.data.DataUtils.writeDataToJson;
import static reka.api.data.DataUtils.writeDataToPrettyJson;
import static reka.util.Util.unsupported;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.BiConsumer;

import reka.api.Hashable;
import reka.api.JsonProvider;
import reka.api.Path;
import reka.api.Path.PathElement;
import reka.api.Path.PathElements;
import reka.api.content.Content;
import reka.core.data.ObjProvider;

import com.google.common.hash.Hasher;

public interface Data extends Iterable<Entry<PathElement,Data>>, JsonProvider, ObjProvider, Hashable {
	
	public static final Data NONE = new NoData();
	
	default OptionalInt getInt(Path path) {
		Optional<Content> c = getContent(path);
		return c.isPresent() ? OptionalInt.of(c.get().asInt()) : OptionalInt.empty();
	}
	
	default Optional<String> getString(Path path) {
		Optional<Content> c = getContent(path);
		return c.isPresent() ? Optional.of(c.get().asUTF8()) : Optional.empty();
	}
	
	default Optional<String> getString(PathElement element) {
		return getString(path(element));
	}
	
	default Optional<String> getString(String key) {
		return getString(PathElements.name(key));
	}
	
	default Optional<String> getString(int index) {
		return getString(PathElements.index(index));
	}
	
	Optional<Content> getContent(Path path);
	
	default Content content() {
		if (isContent()) {
			return getContent(root()).get(); 
		} else {
			throw unsupported("this is not content!");
		}
	}
	
	default Optional<Content> getContent(String key) {
		return getContent(PathElements.name(key));
	}
	
	default Optional<Content> getContent(int index) {
		return getContent(PathElements.index(index));
	}
	
	default Optional<Content> getContent(PathElement element) {
		return getContent(Path.path(element));
	}
	
	default void forEachData(BiConsumer<PathElement,Data> visitor) {
		forEach(e -> {
			visitor.accept(e.getKey(), e.getValue());
		});
	}
	
	void forEachContent(BiConsumer<Path,Content> visitor);
	/*
	default void forEachContent(BiConsumer<Path,Content> visitor) {
		visitEachContent(this, visitor);
	}
	*/
	
	default Optional<Content> firstContent() {
		return Optional.ofNullable(getFirstContent(this));
	}
	
	default void firstContent(BiConsumer<Path,Content> visitor) {
		visitFirstContent(this, visitor);
	}

	Data copy();
	boolean isPresent();
	boolean isMap();
	boolean isList();
	boolean isContent();
	
	default Data at(PathElement element) {
		return at(path(element));
	}
	
	default Data at(String key) {
		return at(PathElements.name(key));
	}
	
	default Data at(int index) {
		return at(PathElements.index(index));
	}

	Data at(Path path);
	
	default Data atFirst(Path... paths) {
		Data result;
		for (Path path : paths) {
			result = at(path);
			if (result.isPresent()) {
				return result;
			}
		}
		return Data.NONE;
	}
	
	default boolean existsAt(PathElement e) {
		return at(e).isPresent();
	}
	
	default boolean existsAt(String key) {
		return at(key).isPresent();
	}
	
	default boolean existsAt(int index) {
		return at(index).isPresent();
	}
	
	default boolean existsAt(Path path) {
		return at(path).isPresent();
	}
	
	boolean contentExistsAt(Path path);
	
	Iterator<Entry<PathElement,Data>> iterator();
	
	Set<PathElement> elements();
	Collection<Data> values();
	int size();
	
	default String toPrettyJson() {
		return writeDataToPrettyJson(this);
	}
	
	default String toJson() {
		return writeDataToJson(this);
	}
	
	default void diffContentTo(Data other, DiffContentConsumer visitor) {
		diffContent(this, other, visitor);
	}
	
	default void diffContentFrom(Data other, DiffContentConsumer visitor) {
		diffContent(other, this, visitor);
	}
	
	default void diffPathTo(Data other, DiffPathConsumer visitor) {
		diffPath(this, other, visitor);
	}
	
	default void diffPathFrom(Data other, DiffPathConsumer visitor) {
		diffPath(other, this, visitor);
	}
	
	default boolean dataEquals(Data other) {
		return dataIsEqual(this, other);
	}
	
	default <T extends MutableData> T copyTo(T data) {
		forEachContent((path, content) -> data.put(path, content));
		return data;
	}

	Map<String, Object> toMap();
	
	default Hasher hash(Hasher hasher) {
		forEachContent((path, content) -> {
			path.hash(hasher);
			content.hash(hasher);
		});
		return hasher;
	}
	
	
}