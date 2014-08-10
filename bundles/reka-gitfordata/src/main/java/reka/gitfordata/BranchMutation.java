package reka.gitfordata;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import reka.api.Path;
import reka.api.content.Content;
import reka.api.data.Data;
import reka.api.data.ListMutation;
import reka.api.data.MapMutation;
import reka.api.data.MutableData;
import reka.api.data.versioned.VersionedAtomicMutableData;
import reka.api.data.versioned.VersionedData.DataVersion;
import reka.core.data.memory.MutableMemoryData;

import com.google.common.util.concurrent.ListenableFuture;

public class BranchMutation implements VersionedAtomicMutableData.Mutation {
	
	private final DataRepoBranch branch;
	private final SHA1DataVersion version;
	
	BranchMutation(DataRepoBranch branch, SHA1DataVersion version) {
		this.branch = branch;
		this.version = version;
	}
	
	private final MutableData added = MutableMemoryData.create();
	private final List<Path> removed = new ArrayList<>();
	
	@Override
	public BranchMutation put(Path path, Content content) {
		added.put(path, content);
		return this;
	}

	@Override
	public BranchMutation put(Path path, Data data) {
		added.put(path, data);
		return this;
	}

	@Override
	public BranchMutation putOrAppend(Path path, Content content) {
		added.putOrAppend(path, content);
		return this;
	}

	@Override
	public BranchMutation putOrAppend(Path path, Data data) {
		added.putOrAppend(path, data);
		return this;
	}

	@Override
	public BranchMutation remove(Path path) {
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
	public BranchMutation putMap(Path path, Consumer<MapMutation> map) {
		added.putMap(path, map);
		return this;
	}

	@Override
	public BranchMutation putList(Path path, Consumer<ListMutation> list) {
		added.putList(path, list);
		return this;
	}

	@Override
	public ListenableFuture<? extends DataVersion> commit() {
		return branch.commit(version, added, removed);
	}


}
