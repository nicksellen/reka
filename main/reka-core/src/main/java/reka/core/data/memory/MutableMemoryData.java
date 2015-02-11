package reka.core.data.memory;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;
import static reka.api.Path.dots;
import static reka.api.Path.path;
import static reka.api.Path.root;
import static reka.api.Path.slashes;
import static reka.api.Path.PathElements.nextIndex;
import static reka.api.content.Contents.booleanValue;
import static reka.api.content.Contents.doubleValue;
import static reka.api.content.Contents.integer;
import static reka.api.content.Contents.longValue;
import static reka.api.content.Contents.nullValue;
import static reka.api.content.Contents.utf8;
import static reka.util.Util.createEntry;
import static reka.util.Util.runtime;
import static reka.util.Util.unchecked;
import static reka.util.Util.unsupported;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.api.Path;
import reka.api.Path.PathElement;
import reka.api.Path.PathElements;
import reka.api.content.Content;
import reka.api.content.types.NullContent;
import reka.api.data.Data;
import reka.api.data.MutableData;
import reka.core.data.ObjBuilder;

public class MutableMemoryData implements MutableDataProvider<Object> {
	
	private final Logger log = LoggerFactory.getLogger(getClass());

	public static MutableData readJson(JsonParser json) {
		throw unsupported("I didn't make this yet!");
	}
	
	public static MutableData create() {
		return new MutableDataWrapper<>(INSTANCE);
	}
	
	public static MutableData from(Data data) {
		return create().merge(data);
	}
	
	public static MutableData create(Consumer<MutableData> consumer) {
		MutableData d = create();
		consumer.accept(d);
		return d;
	}
	
	public static MutableData createFromMap(Map<String,Object> map) {
		return new MutableDataWrapper<>(convertMap(map), MutableMemoryData.INSTANCE);
	}
	
	private static Map<String,Object> convertMap(Map<String,Object> map) {
		for (Entry<String, Object> e : map.entrySet()) {
			e.setValue(convertValue(e.getValue()));
		}
		return map;
	}
	
	@SuppressWarnings("unchecked")
	private static Object convertValue(Object obj) {
		if (obj instanceof Map) {
			return convertMap((Map<String,Object>) obj);
		} else if (obj instanceof List) {
			return convertList((List<Object>) obj);
		} else if (!(obj instanceof Content)) {
			return convertToContent(obj);
		} else {
			return obj;
		}
	}
	
	private static List<Object> convertList(List<Object> list) {
		for (int i = 0; i < list.size(); i++) {
			list.set(i, convertValue(list.get(i)));
		}
		return list;
	}
	
	private static Content convertToContent(Object obj) { 
		if (obj == null) {
			return NullContent.INSTANCE;
		} else if (obj instanceof String) {
			return utf8((String) obj);
		} else if (long.class.isInstance(obj)) {
			return longValue((long) obj);
		} else if (int.class.isInstance(obj)) {
			return integer((int) obj);
		} else if (double.class.isInstance(obj)) {
			return doubleValue((double) obj);
		} else if (boolean.class.isInstance(obj)) {
			return booleanValue((boolean) obj);
		} else {
			throw runtime("don't know how to make %s (%s) a Content", obj, obj.getClass());
		}
	}
	
	public static final MutableMemoryData INSTANCE = new MutableMemoryData();
	
	private MutableMemoryData() {}

	private final JsonFactory jsonFactory = new JsonFactory();
	
	public static void main(String[] args) {
		
		/*
		List<Data> reports = new ArrayList<>();
		
		reports.add(MutableMemoryData.create().putString("name", "first"));
		reports.add(MutableMemoryData.create());
		reports.add(MutableMemoryData.create().putString("name", "third"));
		reports.add(MutableMemoryData.create());
		reports.add(MutableMemoryData.createFromMap(new HashMap<>()));
		reports.add(MutableMemoryData.create().putString("name", "final"));
		
		MutableData b = MutableMemoryData.create();
		b.putList("reports", list -> {
			reports.forEach(report -> {
				list.add(report);
			});
		});
		
		System.out.printf("b: %s\n", b);
		
		b.forEachContent((path, content) -> {
			System.out.printf("%s\n", path);
		});
		
		 *
		 */
		
		AtomicInteger c = new AtomicInteger();
		
		MutableData d1 = MutableMemoryData.create();
		MutableData d2 = MutableMemoryData.create();
		
		d1.putMap("person", m -> {
			m.putString("name", "nick");
			m.putString("interests", "cycling");
		});
		
		System.out.printf("%d: %s\n", c.incrementAndGet(), d1.toPrettyJson());
		
		d1.at(dots("person.interests")).forEachContent((path, content) -> {
			System.out.printf("%s -> %s\n", path.dots(), content);
		});
		
		d1.putOrAppend(dots("person.interests"), utf8("running"));
		d1.putOrAppend(dots("person.interests[+].[+].[+].woah"), utf8("sprinting"));
		
		d1.at(dots("person.interests")).forEachContent((path, content) -> {
			System.out.printf("%s -> %s\n", path.dots(), content);
		});

		
		System.out.printf("%d: %s\n", c.incrementAndGet(), d1.toPrettyJson());
		
		Data b = d1.at(dots("person.interests.nothing.here"));
		
		System.out.printf("b: %s\n", b.toPrettyJson());
		
	}
	
	public void run() {
		
		MutableData a = MutableMemoryData.create();
		MutableData b = MutableMemoryData.create();
		
		a.putString("name", "nick").putInt("age", 25);
		b.putString("name", "nick").putInt("age", 25);
		
		System.out.println(a.equals(b));
		System.out.println(a.dataEquals(b));
		
		Object o = null;
		
		o = put(o, dots("things"), utf8("a nice string!"));
		o = putOrAppend(o, dots("things"), utf8("another nice string!"));
		o = put(o, dots("things2.in.here-again"), utf8("another nice string!"));
		o = putOrAppend(o, dots("things2"), utf8("what about this?"));
		
		log.info("root: {}", o);
		log.info("get: {}", get(o, dots("things2.in.here-again[0]")));
		log.info("json: {}", toPrettyJson(o));
		
		o = put(o, root(), utf8("at the root :)"));
		
		log.info("root!: {}", o);
		
		MutableData w = MutableMemoryData.create();

		w.putMap(dots("yay.a.map"), map -> {
			map.put("nick", utf8("bom!"));
		});
		
		w.putMap(dots("yay"), map -> {
			map.put("b", utf8("hopefully it works!"));
			map.putList("things", list -> {
				list.add(utf8("cricket"));
				list.add(utf8("maths"));
				list.addMap(special -> {
					special.put("with mum", utf8("music"));
					special.put("with dad", utf8("cycling"));
				});
			});
			
		});
		
		w.put(dots("lovely.things.into.here"), utf8("yay"));
		
		w.put(dots("and.here"), utf8("bom"));
		
		log.info("w: {}", w.toPrettyJson());
		//log.info("wa: {}", w.at(dots("lovely.not.here")).toPrettyJson());
		
		w.remove(dots("lovely"));
		
		log.info("w: {}", w.toPrettyJson());
		
		/*
		w.visitContent((path, content) -> {
			log.info("c: {} -> {}", path.dots(), content);
		});
		*/
		
	}
	
	@Override
	public String toPrettyJson(Object obj) {
		return toJson(obj, true);
	}
	
	@Override
	public String toJson(Object obj) {
		return toJson(obj, false);
	}
	
	private String toJson(Object obj, boolean pretty) {
		try {
			StringWriter writer = new StringWriter();
			JsonGenerator json = jsonFactory.createJsonGenerator(writer);
			if (pretty) json.useDefaultPrettyPrinter();
			out(obj, json);
			json.flush();
			return writer.toString();
		} catch (IOException e) {
			throw unchecked(e);
		}
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public Collection<PathElement> elementsOf(Object obj) {
		if (obj instanceof Map) {
			return mapElements((Map<String,Object>) obj);
		} else if (obj instanceof List) {
			return listElements((List<Object>) obj);
		} else {
			return Collections.emptySet();
		}
	}
	
	private Collection<PathElement> mapElements(Map<String,Object> m) {
		return m.keySet().stream().map(PathElements::name).collect(toList());
	}
	
	private Collection<PathElement> listElements(List<Object> l) {
		return range(0, l.size()).mapToObj(PathElements::index).collect(toList());
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public Collection<Object> valuesOf(Object obj) {
		if (obj instanceof Map) {
			return mapValues((Map) obj);
		} else if (obj instanceof List) {
			return listValues((List) obj);
		} else {
			return Collections.emptyList();
		}
	}
	
	private Collection<Object> mapValues(Map<String,Object> m) {
		return m.values();
	}
	
	private Collection<Object> listValues(Collection<Object> l) {
		return l;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void visitEntries(Object obj, BiConsumer<PathElement,Object> visitor) {
		if (obj instanceof Map) {
			mapVisitEntries((Map<String,Object>) obj, visitor);
		} else if (obj instanceof List) {
			listVisitEntries((List<Object>) obj, visitor);
		}
	}
	
	private void mapVisitEntries(Map<String,Object> m, BiConsumer<PathElement,Object> visitor) {
		for (Entry<String, Object> e : m.entrySet()) {
			visitor.accept(PathElements.name(e.getKey()), e.getValue());
		}
	}
	
	private void listVisitEntries(List<Object> l, BiConsumer<PathElement,Object> visitor) {
		for (int i = 0; i < l.size(); i++) {
			visitor.accept(PathElements.index(i), l.get(i));
		}
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public void visitContent(Object obj, Path path, BiConsumer<Path,Content> visitor) {
		if (obj instanceof Map) {
			mapVisitContent((Map<String,Object>) obj, path, visitor);
		} else if (obj instanceof List) {
			listVisitContent((List<Object>) obj, path, visitor);
		} else if (obj instanceof Content) {
			visitor.accept(path, (Content) obj); 
		} else {
			// TODO: should I do this?
			visitor.accept(path, nullValue());
		}
	}
	
	private void mapVisitContent(Map<String,Object> m, Path path, BiConsumer<Path,Content> visitor) {
		for (Entry<String, Object> e : m.entrySet()) {
			visitContent(e.getValue(), path.add(e.getKey()), visitor);
		}
	}
	
	private void listVisitContent(List<Object> l, Path path, BiConsumer<Path,Content> visitor) {
		for (int i = 0; i < l.size(); i++) {
			visitContent(l.get(i), path.add(i), visitor);
		}
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public Iterator<Entry<PathElement,Data>> iterate(Object root) {
		if (root instanceof Map) {
			return mapIterate((Map<String,Object>) root);
		} else if (root instanceof List) {
			return listIterate((List<Object>) root);
		} else {
			return Collections.emptyIterator();
		}
	}
	
	private Iterator<Entry<PathElement,Data>> mapIterate(Map<String,Object> m) {
		return m.entrySet().stream().map(this::entryFor).iterator();
	}
	
	private Entry<PathElement,Data> entryFor(Entry<String,Object> e) {
		return createEntry(PathElements.name(e.getKey()), new DataWrapper<>(e.getValue(), this));
	}
	
	private Iterator<Entry<PathElement,Data>> listIterate(List<Object> l) {
		return new ListIterator(l.iterator());
	}
	

	private final class ListIterator implements Iterator<Entry<PathElement, Data>> {
		
		private final Iterator<Object> it;
		private int i = 0;
		
		ListIterator(Iterator<Object> it) {
			this.it = it;
		}

		@Override
		public boolean hasNext() {
			return it.hasNext();
		}

		@Override
		public Entry<PathElement, Data> next() {
			return createEntry(PathElements.index(i++), new DataWrapper<>(it.next(), INSTANCE));
		}
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public void out(Object o, JsonGenerator json) throws IOException {
		if (o instanceof Map) {
			mapWriteTo((Map<String,Object>) o, json);
		} else if (o instanceof List) {
			listWriteTo((List<Object>) o, json);
		} else if (o instanceof Content) {
			contentWriteTo((Content) o, json);
		} else if (o == null) {
			json.writeNull();
		} else {
			throw runtime("can't write %s (%s) to json", o, o != null ? o.getClass() : "null");
		}
	}
	
	private void mapWriteTo(Map<String,Object> m, JsonGenerator json) throws IOException {
		json.writeStartObject();
		for (Entry<String,Object> e : m.entrySet()) {
			json.writeFieldName(e.getKey());
			out(e.getValue(), json);
		}
		json.writeEndObject();
	}
	
	private void listWriteTo(List<Object> l, JsonGenerator json) throws IOException {
		json.writeStartArray();
		for (Object o : l) {
			out(o, json);
		}
		json.writeEndArray();
	}
	
	private void contentWriteTo(Content c, JsonGenerator json) throws IOException {
		c.writeJsonTo(json);
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public void writeObj(Object o, ObjBuilder builder) {
		if (o instanceof List) {
			listWriteObj((List<Object>) o, builder);
		} else if (o instanceof Map) {
			mapWriteObj((Map<String,Object>) o, builder);
		} else if (o instanceof Content) {
			contentWriteObj((Content) o, builder);
		}
	}
	
	private void listWriteObj(List<Object> l, ObjBuilder builder) {
		builder.writeStartList();
		for (Object o : l) {
			writeObj(o, builder);
		}
		builder.writeEndList();
	}

	private void mapWriteObj(Map<String,Object> m, ObjBuilder builder) {
		builder.writeStartMap();
		for (Entry<String,Object> e : m.entrySet()) {
			builder.writeFieldName(e.getKey());
			writeObj(e.getValue(), builder);
		}
		builder.writeEndMap();
	}

	private void contentWriteObj(Content c, ObjBuilder obj) {
		c.writeObj(obj);
	}

	@Override
	public Object remove(Object obj, Path path) {
		if (path.isEmpty()) return null;
		
		Object root = obj;
		
		PathElement[] es = path.toArray();
		Object[] stack = new Object[es.length];
		stack[0] = root;
		for (int i = 0; i < es.length - 1; i++) {
			obj = get(obj, es[i]);
			stack[i + 1] = obj;
			if (obj == null) return root;
		}
		
		remove(obj, path.last());
		
		for (int i = stack.length - 1; i >= 1; i--) {
			if (sizeOf(stack[i]) == 0) {
				remove(stack[i - 1], es[i - 1]);
			}
		}
		
		return sizeOf(root) > 0 ? root : null;
	}
	
	@SuppressWarnings("unchecked")
	public void remove(Object obj, PathElement e) {
		if (e.isKey() && obj instanceof Map) {
			mapRemove((Map<String,Object>) obj, e.name());
		} else if (e.isIndex() && obj instanceof List) {
			listRemove((List<Object>) obj, e.index());
		}
	}
	
	private void mapRemove(Map<String,Object> m, String key) {
		m.remove(key);
	}
	
	private void listRemove(List<Object> l, int index) {
		if (index < l.size()) l.remove(index);
	}
	
	public Content getContent(Object obj, Path path) {
		Object c = get(obj, path);
		return c instanceof Content ? ((Content) c) : null;
	}
	
	@SuppressWarnings("unchecked")
	private Object get(Object obj, PathElement e) {
		if (e.isKey() && obj instanceof Map) {
			return mapGet((Map<String,Object>) obj, e.name());
		} else if (e.isIndex() && obj instanceof List) {
			return listGet((List<Object>) obj, e.index());
		} else if (e.isEmpty() && obj instanceof Content) {
			return (Content) obj;
		}
		return null;
	}

	private Object listGet(List<Object> l, int i) {
		return i < l.size() ? l.get(i) : null;
	}
	
	private Object mapGet(Map<String,Object> m, String k) {
		return m.get(k);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public int sizeOf(Object obj) {
		if (obj instanceof Map) {
			return ((Map<String,Object>) obj).size();
		} else if (obj instanceof List) {
			return ((List<Object>) obj).size();
		} else {
			return 0;
		}
	}
	
	@Override
	public Object get(Object obj, Path p) {
		for (PathElement e : p) {
			obj = get(obj, e);
			if (obj == null) return null;
		}
		return obj;
	}
	
	@Override
	public Object putOrAppend(Object obj, Path p, Object o) {
		return internalPut(obj, p, o, true);
	}
	
	@Override
	public Object putOrAppendContent(Object obj, Path p, Content content) {
		return putOrAppend(obj, p, content);
	}
	
	@Override
	public Object put(Object obj, Path p, Object o) {
		return internalPut(obj, p, o, false);
	}
	
	@Override
	public Object putContent(Object obj, Path p, Content content) {
		return put(obj, p, content);
	}
	
	private Object internalPut(Object root, Path p, Object o, boolean append) {
		if (p.isEmpty()) return o;
		
		PathElement[] es = p.toArray();
		root = ensureCorrectTypeForElement(root, es[0]);
		
		Object obj = root, objNext;
		PathElement elem = es[0], elemNext;
		
		for (int i = 1; i < es.length; i++) {
			objNext = get(obj, elem);
			elemNext = es[i];

			// TODO need to handle the case append=true here...
			// it's not quite clear what you'd expect with append=true in the middle of your path?
			if (elemNext.isKey() && !(objNext instanceof Map)) {
				objNext = createMap();
				internalPutElement(obj, elem, objNext);				
			} else if (elemNext.isIndexical() && !(objNext instanceof List)) {
				objNext = createList();
				internalPutElement(obj, elem, objNext);
			}
			
			obj = objNext;
			elem = elemNext;
		}
		
		if (append) {
			internalPutOrAppendElement(obj, elem, o);
		} else {
			internalPutElement(obj, elem, o);
		}
		
		return root;
	}
	
	private Object ensureCorrectTypeForElement(Object obj, PathElement e) {
		if (e.isKey() && !(obj instanceof Map)) {
			return createMap();
		} else if (e.isIndexical() && !(obj instanceof List)) {
			return createList();
		} 
		return obj;
	}
	
	@SuppressWarnings("unchecked")
	private void internalPutElement(Object obj, PathElement e, Object o) {
		// MUST have the correct obj for element type already
		if (e.isKey()) {
			mapPut((Map<String,Object>) obj, e.name(), o);
		} else if (e.isIndex()) {
			listSet((List<Object>) obj, e.index(), o);
		} else if (e.isNextIndex()) {
			listAdd((List<Object>) obj, o);
		} else {
			throw new IllegalArgumentException(format("can't put %s -> %s", e, o));
		}
	}
	
	private void internalPutOrAppendElement(Object obj, PathElement e, Object o) {
		// MUST have the correct obj for element type already
		Object existing = get(obj, e);
		if (existing instanceof List) {
			internalPutElement(existing, nextIndex(), o);
		} else if (existing != null) {
			List<Object> l = createList();
			l.add(existing);
			l.add(o);
			internalPutElement(obj, e, l);
		} else {
			internalPutElement(obj, e, o);
		}
	}
	
	private void listAdd(List<Object> l, Object o) {
		l.add(o);
	}
	
	private void listSet(List<Object> l, int i, Object o) {
		if (i == l.size()) {
			l.add(o);
		} else if (i < l.size()) {
			l.set(i, o);
		} else {
			for (int j = l.size(); j < i; j++) {
				l.add(null);
			}
			l.add(o);
		}
	}
	
	@SuppressWarnings("unchecked")
	private void mapPut(Map<String,Object> m, String k, Object o) {
		if (o instanceof Map) {
			Object existing = m.get(k);
			if (existing instanceof Map) {
				mapMerge((Map<String,Object>) existing, (Map<String,Object>) o);
				return;
			}
		}
		m.put(k, o);
	}
	
	private void mapMerge(Map<String,Object> a, Map<String,Object> b) {
		for (Entry<String, Object> e : b.entrySet()) {
			mapPut(a, e.getKey(), e.getValue());
		}
	}

	@Override
	public Object put(Object obj, Path path, Map<String, Object> map) {
		return internalPut(obj, path, map, false);
	}

	@Override
	public Object put(Object obj, Path path, List<Object> list) {
		return internalPut(obj, path, list, false);
	}
	
	@Override
	public Object createEmpty() {
		return null;
	}

	@Override
	public Map<String, Object> createMap() {
		return new LinkedHashMap<>();
	}

	@Override
	public List<Object> createList() {
		return new ArrayList<>();
	}
	
	private List<Object> createList(int length) {
		return new ArrayList<>(length);
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Object clear(Object obj) {
		if (obj instanceof Map) {
			((Map) obj).clear();
		} else if (obj instanceof List) {
			((List) obj).clear();
		} else if (obj instanceof Content) {
			obj = null;
		}
		return obj;
	}

	@Override
	public boolean isPresent(Object obj) {
		return obj != null;
	}
	
	@Override
	public boolean isMap(Object obj) {
		return obj instanceof Map;
	}

	@Override
	public boolean isList(Object obj) {
		return obj instanceof List;
	}

	@Override
	public boolean isContent(Object obj) {
		return obj instanceof Content;
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public Object copy(Object obj) {
		if (obj instanceof Map) {
			return mapCopy((Map) obj);
		} else if (obj instanceof List) {
			return listCopy((List) obj);
		} else {
			return obj; // is this right, just return these ones?
		}
	}
	
	private Object mapCopy(Map<String,Object> map) {
		Map<String,Object> copy = createMap();
		for (Entry<String, Object> e : map.entrySet()) {
			copy.put(e.getKey(), copy(e.getValue()));
		}
		return copy;
	}
	
	private Object listCopy(List<Object> list) {
		List<Object> copy = createList(list.size());
		for (Object o : list) {
			copy.add(copy(o));
		}
		return copy;
	}

	@Override
	public Content content(Object obj) {
		if (obj instanceof Content) {
			return (Content) obj;
		} else {
			return null;
		}
	}

}
