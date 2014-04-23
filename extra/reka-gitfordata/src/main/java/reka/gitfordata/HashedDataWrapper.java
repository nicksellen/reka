package reka.gitfordata;

import static reka.util.Util.unsupported;
import reka.api.data.Data;
import reka.api.data.versioned.ContentChange;
import reka.api.data.versioned.VersionedAtomicMutableData;
import reka.core.data.memory.DataProvider;
import reka.core.data.memory.DataWrapper;
import reka.gitfordata.tree.Record;

public class HashedDataWrapper extends DataWrapper<Record> implements VersionedAtomicMutableData {

	private final DataRepoBranch branch;
	private final SHA1DataVersion version;
	private final Record root;
	private final DataProvider<Record> provider;
	
	public HashedDataWrapper(DataRepoBranch branch, DataProvider<Record> provider, SHA1DataVersion version, Record root) {
		super(root, provider);
		this.branch = branch;
		this.version = version;
		this.root = root;
		this.provider = provider;
	}

	@Override
	public Mutation createMutation() {
		return new BranchMutation(branch, version);
	}

	@Override
	public DataVersion version() {
		return version;
	}

	@Override
	public Iterable<ContentChange> changes() {
		throw unsupported();
	}

	@Override
	public Iterable<ContentChange> changesSince(DataVersion from) {
		throw unsupported();
	}

	@Override
	public Iterable<ContentChange> changesUpTo(DataVersion to) {
		throw unsupported();
	}

	@Override
	public Iterable<ContentChange> changesFor(DataVersion to) {
		throw unsupported();
	}

	@Override
	public Iterable<ContentChange> changesBetween(DataVersion from, DataVersion to) {
		throw unsupported();
	}

	@Override
	public Data snapshot() {
		return new DataWrapper<>(root, provider);
	}

}
