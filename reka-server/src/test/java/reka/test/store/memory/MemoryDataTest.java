package reka.test.store.memory;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static reka.api.Path.dots;
import static reka.api.Path.path;
import static reka.api.Path.root;
import static reka.api.Path.PathElements.index;
import static reka.api.Path.PathElements.name;
import static reka.data.content.Contents.integer;
import static reka.data.content.Contents.utf8;

import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.api.Path;
import reka.api.Path.PathElement;
import reka.data.Data;
import reka.data.MutableData;
import reka.data.content.Content;
import reka.data.content.Contents;
import reka.data.memory.MutableMemoryData;

public class MemoryDataTest {
	
	private final Logger log = LoggerFactory.getLogger(getClass());

	@Test
	public void canBeCreated() {
		final MutableData store = MutableMemoryData.create();
		int index = 0;
		
		store.put(Path.newBuilder().add("people").add(index).add("name").build(), Contents.utf8("Nick"));
		store.put(Path.newBuilder().add("people").add(index).add("age").build(), Contents.integer(29));
		index++;
	
		store.put(Path.newBuilder().add("people").add(index).add("name").build(), Contents.utf8("Jason"));
		store.put(Path.newBuilder().add("people").add(index).add("age").build(), Contents.integer(51));
		index++;
		
		store.put(Path.newBuilder().add("people").add(index).add("name").build(), Contents.utf8("Alex"));
		store.put(Path.newBuilder().add("people").add(index).add("age").build(), Contents.integer(23));
		index++;

		Content name1 = store.getContent(Path.newBuilder().add("people").add(1).add("name").build()).get();
		assertThat(name1.asUTF8(), equalTo("Jason"));
		
		Content age2 = store.getContent(Path.newBuilder().add("people").add(2).add("age").build()).get();
		assertThat(age2.asInt(), equalTo(23));
	}
	
	@Test
	public void childItemsWork() {

		MutableData store = MutableMemoryData.create();
		store.put(path("things", "for", "me"), Contents.utf8("yay"));
		
		Data forChild = store.at(path("things", "for"));
		assertThat(forChild.getString(path("me")).get(), equalTo("yay"));
		assertThat(store.at(path("things", "for", "me")).getString(root()).get(), equalTo("yay"));
		
		store.createMapAt(path("things", "for")).putString(path("me"), "yay changed");
		
		assertThat(store.getString(path("things", "for", "me")).get(), equalTo("yay changed"));
		assertThat(forChild.getString(path("me")).get(), equalTo("yay changed"));
	}
	
	@Test
	public void canRemoveThings() {
		MutableData store = MutableMemoryData.create();
		
		Path p = path("something", "to", "remove");
		
		store.putString(p, "bom");
		assertThat(store.getString(p).get(), equalTo("bom"));
		
		store.remove(p);
		assertFalse(store.getString(p).isPresent());
		
	}
	
	@Test
	public void canBeTurnedIntoAMap() {
		MutableData store = MutableMemoryData.create();
		
		store.put(path("name", "oh", "this"), utf8("nick"));
		store.put(path("age"), integer(25));
		store.put(path(name("items"), index(0)), utf8("cows"));
		store.put(path(name("items"), index(1)), utf8("horses"));
		store.put(path(name("items"), index(2), name("type")), utf8("sheep"));
		
		log.debug("map: {}\n", store.toMap());
		
	}
	
	@Test
	public void canIterate() {
		
		MutableData store = MutableMemoryData.create();
		
		store.put(path("name", "oh", "this"), utf8("nick"));
		store.put(path("age"), integer(25));
		store.put(path(name("items"), index(0)), utf8("cows"));
		store.put(path(name("items"), index(1)), utf8("horses"));
		store.put(path(name("items"), index(2), name("type")), utf8("sheep"));
		
		AtomicInteger entryCount = new AtomicInteger();
		store.at(dots("name.oh")).forEachContent((path, content) -> {
			entryCount.incrementAndGet();
			log.debug("{} -> [{}]\n", path.dots(), content.value());
		});
		assertThat(entryCount.get(), equalTo(1));
		
		entryCount.set(0);
		//for (Entry<Path, Content> entry : store.dataAt(path("items"))) {
		store.at(path("items")).forEachContent((path, content) -> {
			entryCount.incrementAndGet();
			log.debug("{} -> [{}]\n", path.dots(), content.value());
		});
		assertThat(entryCount.get(), equalTo(3));
		
	}
	
	@Test
	public void canIterateASingleValue() {
		MutableData store = MutableMemoryData.create();
		
		Path path = path("name", "oh", "this"); 
		
		store.put(path, utf8("nick"));

		AtomicInteger entryCount = new AtomicInteger();
		//for (Entry<Path, Content> entry : store.dataAt(path)) {
		store.at(path).forEachContent((p, content) -> {
			entryCount.incrementAndGet();
			log.debug("{} ({}) -> [{}]\n", p.dots(), p.isEmpty(), content.value());
		});
		assertThat(entryCount.get(), equalTo(1));
		
		Data dsWithSingleValue = store.at(path);
		
		Optional<Content> c = dsWithSingleValue.getContent(path("noway"));
		
		assertFalse(c.isPresent());
		
	}
	
	@Test
	public void tryAddingToList() {
		MutableData store = MutableMemoryData.create();
		
		Path path = path("items");
		
		store.putString(path("something"), "unused");
		
		// first one goes in as a normal value
		store.putOrAppend(path, utf8("pig"));
		assertThat(store.getString(path).get(), equalTo("pig"));
		log.debug(store.toPrettyJson());
		
		// next one should force a new array to be created with both values in it
		store.putOrAppend(path, utf8("donkey"));
		log.debug(store.toPrettyJson());
		assertThat(store.getString(path.add(0)).get(), equalTo("pig"));
		assertThat(store.getString(path.add(1)).get(), equalTo("donkey"));
		
		for (Entry<PathElement, Data> entry : store.at(path)) {
			Path cPath = path.add(entry.getKey());
			log.debug("{}\n", cPath.dots());
		}
		

		store.put(path.add(1).add("animal"), utf8("donkey"));
		
		
		log.debug("\n\n------- A ------\n\n");
		
		Path p2 = path.add(1);
		for (Entry<PathElement,Data> entry : store.at(p2)) {
			log.debug("got store at {}\n", entry.getKey());
			PathElement pe = entry.getKey();
			Path cPath = p2.add(pe);
			log.debug("{}\n", cPath.dots());
		}

		log.debug("\n\n------- B ------\n\n");
		
		Data p2Store = store.at(p2);
		for (PathElement pe : p2Store.elements()) {
			log.debug("fetching store at {}\n", path(pe));
			Path cPath = p2.add(pe);
			log.debug("{}\n", cPath.dots());
		}
	}
	
	@Test
	public void canSetAWholeNestedDataObject() {
		MutableData data1 = MutableMemoryData.create();
		MutableData data2 = MutableMemoryData.create();
		
		Path inner = dots("something.in.here");
		Path outer = dots("nested.item");
		
		data2.put(inner, utf8("yay"));
		data1.put(outer, data2);
		assertThat(data1.getString(outer.add(inner)).orElse("not found"), equalTo("yay"));
	}
		
	@Test
	public void canAddOrAppendWholeDatas() {
		MutableData data = MutableMemoryData.create();
		
		Path base = dots("things.in.here");
		data.putOrAppend(base, MutableMemoryData.create()
			.putString(path("name"), "Nick")
			.putInt(path("age"), 29));

		assertThat(data.getString(base.add("name")).get(), equalTo("Nick"));
		assertThat(data.getInt(base.add("age")).getAsInt(), equalTo(29));
		
		data.putOrAppend(base, MutableMemoryData.create()
				.putString(path("name"), "Nigel")
				.putInt(path("age"), 42));

		assertThat(data.getString(base.add(0).add("name")).get(), equalTo("Nick"));
		assertThat(data.getInt(base.add(0).add("age")).getAsInt(), equalTo(29));

		assertThat(data.getString(base.add(1).add("name")).get(), equalTo("Nigel"));
		assertThat(data.getInt(base.add(1).add("age")).getAsInt(), equalTo(42));
	}
	
}
