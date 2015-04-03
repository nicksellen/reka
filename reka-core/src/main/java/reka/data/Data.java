package reka.data;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static reka.data.DataUtils.dataIsEqual;
import static reka.data.DataUtils.diffContent;
import static reka.data.DataUtils.diffPath;
import static reka.data.DataUtils.getFirstContent;
import static reka.data.DataUtils.visitFirstContent;
import static reka.data.DataUtils.writeDataToJson;
import static reka.data.DataUtils.writeDataToPrettyJson;
import static reka.util.Path.path;
import static reka.util.Path.root;
import static reka.util.Util.unsupported;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import reka.data.content.Content;
import reka.util.Hashable;
import reka.util.JsonProvider;
import reka.util.Path;
import reka.util.Path.PathElement;
import reka.util.Path.PathElements;

import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;

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
	
	default void ifPresent(Consumer<Data> consumer) {
		if (isPresent()) consumer.accept(this);
	}
	
	default void ifContent(Consumer<Content> consumer) {
		if (isContent()) consumer.accept(this.content());
	}
	
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
	
	Collection<PathElement> elements();
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
	
	default Object view() {
		return DataViewUtil.convert(this);
	}
	
	default Map<String,Object> viewAsMap() {
		if (!isPresent()) return emptyMap();
		checkState(isMap(), "not a map");
		return new DataMapView(this);
	}
	
	default List<Object> viewAsList() {
		if (!isPresent()) return emptyList();
		checkState(isMap(), "not a list");
		return new DataListView(this);
	}

	Map<String, Object> toMap();
	
	default Hasher hash(Hasher hasher) {
		forEachContent((path, content) -> {
			path.hash(hasher);
			content.hash(hasher);
		});
		return hasher;
	}
	
	default int defaultHashCode() {
		return hash(Hashing.crc32().newHasher()).hash().asInt();
	}
	
	default boolean defaultEquals(Object obj) {
		if (!(obj instanceof Data)) return false;
		Data other = (Data) obj;
		return dataEquals(other);
	}
	
	
}