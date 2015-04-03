package reka.data.memory;

import static com.google.common.base.Preconditions.checkArgument;
import static reka.api.Path.path;
import static reka.api.Path.PathElements.nextIndex;
import static reka.util.Util.runtime;
import static reka.util.Util.unchecked;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import reka.api.Path;
import reka.api.Path.PathElement;
import reka.api.Path.PathElements;
import reka.data.Data;
import reka.data.ListMutation;
import reka.data.MapMutation;
import reka.data.MutableData;
import reka.data.content.Content;
import reka.util.ThrowingConsumer;

public class MutableDataWrapper<T> extends DataWrapper<T> implements Data, MutableData {

	private final MutableDataProvider<T> provider;
	
	public MutableDataWrapper(MutableDataProvider<T> provider) {
		super(provider);
		this.provider = provider;
	}
	
	public MutableDataWrapper(T root, MutableDataProvider<T> provider) {
		super(root, provider);
		this.provider = provider;
	}
	
	public void clear() {
		root = provider.clear(root);
	}
	
	public MutableData put(Path path, Data data) {
		MutableDataWrapper<T> other = otherOrNull(data);
		if (other != null) {
			root = provider.put(root, path, other.root);
		} else {
			mutableAt(path).merge(data);
		}
		return this;
	}
	
	@SuppressWarnings("unchecked")
	private MutableDataWrapper<T> otherOrNull(Data data) {
		if (data instanceof MutableDataWrapper) {
			MutableDataWrapper<T> other = (MutableDataWrapper<T>) data;
			if (other.provider.equals(provider)) {
				return other;
			}
		}
		return null;
	}

	@Override
	public MutableData putOrAppend(Path path, Data data) {
		MutableDataWrapper<T> other = otherOrNull(data); 
		if (other != null) {
			root = provider.putOrAppend(root, path, other.root);
		} else {
			throw runtime("we don't currently handle putOrAppend'ing non MutableDataWrapper<T> data");
		}
		return this;
	}

	@Override
	public MutableData put(Path path, Content content) {
		root = provider.putContent(root, path, content);
		return this;
	}
	
	@Override
	public MutableData putOrAppend(Path path, Content content) {
		root = provider.putOrAppendContent(root, path, content);
		return this;
	}
	
	public MutableData remove(Path path) {
		root = provider.remove(root, path);
		return this;
	}
	
	public Optional<Content> getContent(Path path) {
		T content = provider.get(root, path);
		if (content != null && content instanceof Content) {
			return Optional.of((Content) content);
		} else {
			return Optional.empty();
		}
	}

	@Override	
	public MutableData mutableAt(Path path) {
		return new MutableDataWrapper<T>(provider.get(root, path), provider);
	}
	
	@Override
	public MutableData createMapAt(Path path) {
		T obj = provider.createMap();
		root = provider.put(root, path, obj);
		return mutableAt(path);
	}
	
	@Override
	public MutableData createListAt(Path path) {
		T obj = provider.createList();
		root = provider.put(root, path, obj);
		return mutableAt(path);
	}
	
	@Override
	public MutableData putMap(Path path, ThrowingConsumer<MapMutation> consumer) {
		BatchMutateMap mutation = new BatchMutateMap(provider.createMap());
		try {
			consumer.accept(mutation);
			for (Path p : mutation.removed) {
				 provider.remove(root, path.add(p));
			}
			root = provider.put(root, path, mutation.map);
		} catch (Exception t) {
			throw unchecked(t);
		}
		return this;
	}

	@Override
	public MutableData putList(Path path, ThrowingConsumer<ListMutation> consumer) {
		BatchMutateList mutation = new BatchMutateList(provider.createList());
		try {
			consumer.accept(mutation);
			for (PathElement e : mutation.removed) {
				 provider.remove(root, path(e));
			}
			root = provider.put(root, path, mutation.list);
		} catch (Exception t) {
			throw unchecked(t);
		}
		return this;
	}

	@Override
	public MutableData mutableCopy() {
		T copy = provider.copy(root);
		return new MutableDataWrapper<T>(copy, provider);
	}
	
	@Override
	public String toString() {
		return toJson();
	}
	
	private final class BatchMutateList implements ListMutation {
		
		private final List<PathElement> removed = new ArrayList<>();
		
		private volatile T list;
		
		BatchMutateList(T list) {
			this.list = list;
		}
		
		@Override
		public ListMutation remove(int index) {
			removed.add(PathElements.index(index));
			return this;
		}

		@Override
		public ListMutation add(Content content) {
			list = provider.putContent(list, path(nextIndex()), content);
			return this;
		}

		@Override
		public ListMutation add(Data data) {
			MutableDataWrapper<T> other = otherOrNull(data);
			checkArgument(other != null, "we only support putting MemoryData in (for now), not %s", data.getClass());
			list = provider.put(list, path(nextIndex()), other.root);
			return this;
		}


		@Override
		public ListMutation addList(ThrowingConsumer<ListMutation> consumer) {
			BatchMutateList mutation = new BatchMutateList(provider.createList());
			try {
				consumer.accept(mutation);
				provider.put(list, path(nextIndex()), mutation.list);
			} catch (Exception t) {
				throw unchecked(t);
			}
			return this;
		}

		@Override
		public ListMutation addMap(ThrowingConsumer<MapMutation> consumer) {
			BatchMutateMap mutation = new BatchMutateMap(provider.createMap());
			try {
				consumer.accept(mutation);
				provider.put(list, path(nextIndex()), mutation.map);
			} catch (Exception t) {
				throw unchecked(t);
			}
			return this;
		}
		
	}
	
	private final class BatchMutateMap implements MapMutation {
		
		private final List<Path> removed = new ArrayList<>();
		
		private volatile T map;
		
		BatchMutateMap(T map) {
			this.map = map;
		}

		@Override
		public MapMutation remove(Path path) {
			removed.add(path);
			return this;
		}

		@Override
		public MapMutation put(Path path, Data data) {
			MutableDataWrapper<T> other = otherOrNull(data);
			checkArgument(other != null, "we only support putting MemoryData in (for now)");
			provider.put(map, path, other.root);
			return this;
		}

		@Override
		public MapMutation put(Path path, Content content) {
			provider.putContent(map, path, content);
			return this;
		}

		@Override
		public MapMutation putList(Path path,ThrowingConsumer<ListMutation> consumer) {
			BatchMutateList mutation = new BatchMutateList(provider.createList());
			try {
				consumer.accept(mutation);
				provider.put(map, path, mutation.list);
			} catch (Exception t) {
				throw unchecked(t);
			}
			return this;
		}

		@Override
		public MapMutation putMap(Path path, ThrowingConsumer<MapMutation> consumer) {
			BatchMutateMap mutation = new BatchMutateMap(provider.createMap());
			try {
				consumer.accept(mutation);
				provider.put(map, path, mutation.map);
			} catch (Exception t) {
				throw unchecked(t);
			}
			return this;
		}
	}

	
}