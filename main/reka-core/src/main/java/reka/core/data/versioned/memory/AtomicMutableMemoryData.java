package reka.core.data.versioned.memory;

import static reka.api.Path.dots;
import static reka.api.content.Contents.utf8;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.StampedLock;
import java.util.function.BiConsumer;

import org.codehaus.jackson.JsonGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.api.Path;
import reka.api.Path.PathElement;
import reka.api.content.Content;
import reka.api.data.AtomicMutableData;
import reka.api.data.Data;
import reka.api.data.DataMutation;
import reka.api.data.MutableData;
import reka.core.data.ObjBuilder;
import reka.core.data.memory.MutableMemoryData;

public class AtomicMutableMemoryData implements AtomicMutableData {
	
	private static final Logger log = LoggerFactory.getLogger(AtomicMutableMemoryData.class);

	private final StampedLock lock = new StampedLock();
	private final MutableData data;
	
	public AtomicMutableMemoryData() {
		this(MutableMemoryData.create());
	}
	
	private AtomicMutableMemoryData(MutableData data) {
		this.data = data;
	}
	
	public static void main(String[] args) throws InterruptedException, ExecutionException {
		
		AtomicMutableData data1 = new AtomicMutableMemoryData();
		
		data1.createMutation()
				.put(dots("lots.of.things.inside.here"), utf8("yay"))
				.putString("name", "nick")
				.putMap("interests", map -> {
					map.putString("running", "yes");
					map.putString("cycling", "yes");
				})
			.commit().get();

		AtomicMutableData data2 = data1.atomicMutableCopy();

		log.debug(data2.getString(dots("lots.of.things.inside.here")).orElse("missing"));
		
		Mutation mutation = data2.createMutation()
				.putString("apples", "are nice")
				.remove(dots("lots.of.things"))
				.remove(dots("interests.running"));
		
		log.debug(data2.getString(dots("lots.of.things.inside.here")).orElse("missing"));
		
		mutation.commit().get();

		log.debug("json1: {}", data1.toPrettyJson());
		log.debug("json2: {}", data2.toPrettyJson());
		
		data1.diffContentTo(data2, (path, type, prev, cur) -> {
			log.debug("{}:{} {} -> {}", path, type, prev, cur);
		});
		
		
	}

	@Override
	public Optional<Content> getContent(Path path) {
		long stamp = lock.readLock();
		try {
			return data.getContent(path);	
		} finally {
			lock.unlock(stamp);
		}
	}

	@Override
	public Data copy() {
		long stamp = lock.readLock();
		try {
			return data.copy();
		} finally {
			lock.unlock(stamp);
		}
	}

	@Override
	public boolean isPresent() {
		long stamp = lock.readLock();
		try {
			return data.isPresent();
		} finally {
			lock.unlock(stamp);
		}
	}

	@Override
	public boolean isMap() {
		long stamp = lock.readLock();
		try {
			return data.isMap();
		} finally {
			lock.unlock(stamp);
		}
	}

	@Override
	public boolean isList() {
		long stamp = lock.readLock();
		try {
			return data.isList();
		} finally {
			lock.unlock(stamp);
		}
	}

	@Override
	public boolean isContent() {
		long stamp = lock.readLock();
		try {
			return data.isContent();
		} finally {
			lock.unlock(stamp);
		}
	}

	@Override
	public Data at(Path path) {
		long stamp = lock.readLock();
		try {
			return data.at(path);
		} finally {
			lock.unlock(stamp);
		}
	}

	@Override
	public Iterator<Entry<PathElement, Data>> iterator() {
		long stamp = lock.readLock();
		try {
			return data.iterator();	
		} finally {
			lock.unlock(stamp);
		}
	}

	@Override
	public Set<PathElement> elements() {
		long stamp = lock.readLock();
		try {
			return data.elements();
		} finally {
			lock.unlock(stamp);
		}
	}

	@Override
	public Collection<Data> values() {
		long stamp = lock.readLock();
		try {
			return data.values();
		} finally {
			lock.unlock(stamp);
		}
	}

	@Override
	public int size() {
		long stamp = lock.readLock();
		try {
			return data.size();
		} finally {
			lock.unlock(stamp);
		}
	}

	@Override
	public void out(JsonGenerator json) throws IOException {
		long stamp = lock.readLock();
		try {
			data.out(json);
		} finally {
			lock.unlock(stamp);
		}
	}

	@Override
	public void writeObj(ObjBuilder obj) {
		long stamp = lock.readLock();
		try {
			data.writeObj(obj);
		} finally {
			lock.unlock(stamp);
		}
	}

	@Override
	public DataMutation<Mutation> createMutation() {
		return new VAMMutation(this);
	}

	public void merge(MutableData added, List<Path> removed) {
		long stamp = lock.writeLock();
		try {
			for (Path path : removed) {
				data.remove(path);
			}
			data.merge(added);
		} finally {
			lock.unlock(stamp);
		}
	}

	@Override
	public AtomicMutableData atomicMutableCopy() {
		return vamCopy();
	}
	
	private AtomicMutableMemoryData vamCopy() {
		return new AtomicMutableMemoryData(data.mutableCopy());
	}

	@Override
	public void forEachContent(BiConsumer<Path, Content> visitor) {
		long stamp = lock.readLock();
		try {
			data.forEachContent(visitor);
		} finally {
			lock.unlock(stamp);
		}
	}

	@Override
	public boolean contentExistsAt(Path path) {
		long stamp = lock.readLock();
		try {
			return data.contentExistsAt(path);
		} finally {
			lock.unlock(stamp);
		}
	}

	@Override
	public Map<String, Object> toMap() {
		long stamp = lock.readLock();
		try {
			return data.toMap();
		} finally {
			lock.unlock(stamp);
		}
	}

}
