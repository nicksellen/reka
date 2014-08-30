package reka.core.data.memory;

import static com.google.common.base.Preconditions.checkState;
import static java.util.stream.Collectors.toList;
import static reka.api.Path.path;
import static reka.api.Path.root;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.BiConsumer;

import org.codehaus.jackson.JsonGenerator;

import reka.api.Path;
import reka.api.Path.PathElement;
import reka.api.Path.PathElements;
import reka.api.content.Content;
import reka.api.data.Data;
import reka.core.data.DefaultObjBuilder;
import reka.core.data.ObjBuilder;

public class DataWrapper<T> implements Data {

	private final DataProvider<T> provider;
	protected volatile T root;
	
	public DataWrapper(DataProvider<T> provider) {
		this.root = provider.createEmpty();
		this.provider = provider;
	}
	
	public DataWrapper(T root, DataProvider<T> provider) {
		this.root = root;
		this.provider = provider;
	}

	@Override
	public Optional<Content> getContent(Path path) {
		return Optional.ofNullable(provider.getContent(root, path));
	}

	@Override
	public Data at(Path path) {
		return new DataWrapper<T>(provider.get(root, path), provider);
	}
	
	@Override
	public Data at(PathElement element) {
		return at(path(element));
	}

	@Override
	public boolean existsAt(Path path) {
		if (root == null) return false;
		return provider.existsAt(root, path);
	}

	@Override
	public boolean contentExistsAt(Path path) {
		if (root == null) return false;
		return provider.contentExistsAt(root, path);
	}

	@Override
	public Data at(String key) {
		return at(PathElements.name(key));
	}
	
	@Override
	public Data at(int index) {
		return at(PathElements.index(index));
	}

	@Override
	public String toPrettyJson() {
		if (root == null) return "{}";
		return provider.toPrettyJson(root);
	}

	@Override
	public String toJson() {
		if (root == null) return "{}";
		return provider.toJson(root);
	}

	@Override
	public Iterator<Entry<PathElement,Data>> iterator() {
		if (root == null) return Collections.emptyIterator();
		return provider.iterate(root);
	}
	
	/*
	private void forEach(BiConsumer<PathElement,T> visitor) {
		if (root == null) return;
		provider.visitEntries(root, visitor);
	}
	*/

	@Override
	public Collection<PathElement> elements() {
		if (root == null) return Collections.emptySet();
		return provider.elementsOf(root);
	}

	@Override
	public Collection<Data> values() {
		if (root == null) return Collections.emptyList();
		return provider.valuesOf(root).stream().map(this::toWrapper).collect(toList());
	}
	
	private DataWrapper<T> toWrapper(T obj) {
		return new DataWrapper<T>(obj, provider);
	}
	
	public void visitContent(BiConsumer<Path,Content> visitor) {
		if (root == null) return;
		provider.visitContent(root, root(), visitor);
	}
	
	public Content content() {
		if (root == null) return null;
		return provider.content(root);
	}

	@Override
	public void writeJsonTo(JsonGenerator json) throws IOException {
		if (root == null) {
			json.writeStartObject();
			json.writeEndObject();
		} else {
			provider.out(root, json);
		}
	}

	@Override
	public void writeObj(ObjBuilder obj) {
		if (root == null) return;
		provider.writeObj(root, obj);
	}

	@Override
	public boolean isPresent() {
		if (root == null) return false;
		return provider.isPresent(root);
	}

	@Override
	public boolean isMap() {
		if (root == null) return false;
		return provider.isMap(root);
	}

	@Override
	public boolean isList() {
		if (root == null) return false;
		return provider.isList(root);
	}

	@Override
	public boolean isContent() {
		if (root == null) return false;
		return provider.isContent(root);
	}

	@Override
	public int size() {
		if (root == null) return 0;
		return provider.sizeOf(root);
	}

	@Override
	public Data copy() {
		return new DataWrapper<>(provider.copy(root), provider);
	}

	@Override
	public void forEachContent(BiConsumer<Path, Content> visitor) {
		if (root == null) return;
		provider.visitContent(root, root(), visitor);
	}

	@SuppressWarnings("unchecked")
	@Override
	public Map<String, Object> toMap() {
		checkState(isMap(), "can only convert map data to maps");
		ObjBuilder obj = new DefaultObjBuilder();
		writeObj(obj);
		return (Map<String,Object>) obj.obj();
	}
	
}