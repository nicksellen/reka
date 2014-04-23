package reka.core.data.versioned.memory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import reka.api.Path;
import reka.api.content.Content;
import reka.api.data.AtomicMutableData.Mutation;
import reka.api.data.Data;
import reka.api.data.ListMutation;
import reka.api.data.MapMutation;
import reka.api.data.MutableData;
import reka.core.data.memory.MutableMemoryData;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

public class VAMMutation implements Mutation {

	private final AtomicMutableMemoryData parent;
	
	private final MutableData added = MutableMemoryData.create();
	private final List<Path> removed = new ArrayList<>();

	VAMMutation(AtomicMutableMemoryData parent) {
		this.parent = parent;
	}
	
	@Override
	public Mutation put(Path path, Content content) {
		added.put(path, content);
		return this;
	}

	@Override
	public Mutation put(Path path, Data data) {
		added.put(path, data);
		return this;
	}

	@Override
	public Mutation putOrAppend(Path path, Content content) {
		added.putOrAppend(path, content);
		return this;
	}

	@Override
	public Mutation putOrAppend(Path path, Data data) {
		added.putOrAppend(path, data);
		return this;
	}

	@Override
	public Mutation remove(Path path) {
		added.remove(path);
		removed.add(path);
		return this;
	}

	@Override
	public MutableData createMapAt(Path path) {
		return added.createMapAt(path);
	}

	@Override
	public MutableData createListAt(Path path) {
		return added.createListAt(path);
	}

	@Override
	public Mutation putMap(Path path, Consumer<MapMutation> map) {
		added.putMap(path, map);
		return this;
	}

	@Override
	public Mutation putList(Path path, Consumer<ListMutation> list) {
		added.putList(path, list);
		return this;
	}

	@Override
	public ListenableFuture<Void> commit() {
		parent.merge(added, removed);
		return Futures.immediateFuture(null);
	}
	

}
