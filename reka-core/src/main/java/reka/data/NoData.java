package reka.data;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;

import org.codehaus.jackson.JsonGenerator;

import reka.api.Path;
import reka.api.Path.PathElement;
import reka.data.content.Content;

final class NoData implements Data {

	public static final Data INSTANCE = new NoData();
	public static final Iterator<Entry<PathElement,Data>> ITERATOR = Collections.emptyIterator();
	public static final Set<PathElement> ELEMENTS = Collections.emptySet();
	public static final Collection<Data> VALUES = Collections.emptyList();

	@Override
	public boolean isPresent() {
		return false;
	}
	@Override
	public boolean isMap() {
		return false;
	}

	@Override
	public boolean isList() {
		return false;
	}
	
	@Override
	public boolean isContent() {
		return false;
	}

	@Override
	public Data at(PathElement path) {
		return INSTANCE;
	}

	@Override
	public Iterator<Entry<PathElement,Data>> iterator() {
		return ITERATOR;
	}

	@Override
	public void writeJsonTo(JsonGenerator json) throws IOException {
		json.writeNull();
	}

	@Override
	public Set<PathElement> elements() {
		return ELEMENTS;
	}

	@Override
	public Collection<Data> values() {
		return VALUES;
	}

	@Override
	public int size() {
		return 0;
	}
	
	@Override
	public Data copy() {
		return this;
	}

	@Override
	public void writeObj(ObjBuilder obj) {
	}
	
	@Override
	public Optional<Content> getContent(Path path) {
		return Optional.empty();
	}
	@Override
	public Data at(Path path) {
		return this;
	}
	@Override
	public void forEachContent(BiConsumer<Path, Content> visitor) {
		// nothing to visit!
	}
	@Override
	public boolean contentExistsAt(Path path) {
		return false;
	}
	@Override
	public Map<String, Object> toMap() {
		return Collections.emptyMap();
	}
	
}