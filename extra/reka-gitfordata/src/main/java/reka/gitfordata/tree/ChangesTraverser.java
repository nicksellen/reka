package reka.gitfordata.tree;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import reka.api.Path;
import reka.api.Path.PathElement;
import reka.api.data.versioned.ContentChange;
import reka.gitfordata.tree.record.CollectionRecord;
import reka.gitfordata.tree.record.ContentRecord;

public class ChangesTraverser implements Iterable<ContentChange> {

	private final RecordReader reader;
	private final CollectionRecord fromCollection;
	private final CollectionRecord toCollection;
	
	public ChangesTraverser(RecordReader reader, CollectionRecord from, CollectionRecord to) {
		this.reader = reader;
		this.fromCollection = from;
		this.toCollection = to;
	}
	
	@Override
	public Iterator<ContentChange> iterator() {
		return new ChangesTraverserIterator();
	}
	
	private class ChangesTraverserIterator implements Iterator<ContentChange> {
	
		private final Deque<Level> stack = new ArrayDeque<>();
		private final Deque<PathElement> paths = new ArrayDeque<>();
		
		private ContentChange next;
		
		public ChangesTraverserIterator() {
			push(null, new Level(fromCollection, toCollection));
		}
		
		private class TreeLevelEntry {
			final PathElement key;
			final ObjectIdAndRecord<?> from;
			final ObjectIdAndRecord<?> to;
			TreeLevelEntry(PathElement path, ObjectIdAndRecord<?> left, ObjectIdAndRecord<?> right) {
				this.key = path;
				this.from = left;
				this.to = right;
			}
		}
		
		private class Level implements Iterator<TreeLevelEntry> {
					
			private final CollectionRecord fromTree;
			private final CollectionRecord toTree;
	
			private final Iterator<PathElement> keys;
			
			private TreeLevelEntry next;
			
			private Level(CollectionRecord fromTree, CollectionRecord toTree) {
				this.fromTree = fromTree;
				this.toTree = toTree;
	
				Set<PathElement> allKeys = new TreeSet<>();
				
				if (fromTree != null) {
					for (PathElement k : fromTree.children().keySet()) {
						allKeys.add(k);
					}
				}
				
				if (toTree != null) {
					for (PathElement k : toTree.children().keySet()) {
						allKeys.add(k);
					}
				}
				
				keys = allKeys.iterator();
			}
	
			private void findNext() {
				if (next != null || !keys.hasNext()) return;
				PathElement key = keys.next();
				next = new TreeLevelEntry(key, childOrNull(fromTree, key), childOrNull(toTree, key));
			}
			
			@Override
			public boolean hasNext() {
				findNext();
				return next != null;
			}
	
			@Override
			public TreeLevelEntry next() {
				findNext();
				TreeLevelEntry value = next;
				next = null;
				return value;
			}
	
			@Override
			public void remove() {
				throw new UnsupportedOperationException("denied :)");
			}
			
		}
		
		private void push(PathElement path, Level tl) {
			stack.push(tl);
			if (path != null) {
				paths.push(path);
			}
		}
		
		private void restack() {
			while (!stack.isEmpty() && !stack.peek().hasNext()) {
				stack.pop();
				if (!paths.isEmpty()) {
					paths.pop();
				}
			}
		}
		
		public Path makePath(PathElement last) {
			return Path.newBuilder().add(paths.descendingIterator()).add(last).build();
		}
		
		private void findNext() {
			
			while (next == null && !stack.isEmpty() && stack.peek().hasNext()) {
			
				TreeLevelEntry entry = stack.peek().next();
				
				ObjectIdAndRecord<?> from = entry.from;
				ObjectIdAndRecord<?> to = entry.to;
				
				if (from != null && to != null && from.equals(to)) {
					
					// no change
					
				} else if (from == null) {
					
					// new
					
					if (to.record() instanceof ContentRecord) {
						next = new ContentChange.Added(makePath(entry.key),((ContentRecord) to.record()).toContent());
					}
					
				} else if (to == null) {
					
					// removed
					
					if (from.record() instanceof ContentRecord) {
						next = new ContentChange.Removed(makePath(entry.key),((ContentRecord) from.record()).toContent());
					}
					
				} else {
					
					// changed 
					
					// (modified is only TreeContent > TreeContent
					//  other things are either counted as added or removed)
					
					// TODO: add the consideration that if we're filtering for a certain type 
					// if the thing changes type to/from our filtered types as far as we're concerned it
					// has been added/removed (this might not be the place to make that happen though).
					
					if (to.record() instanceof ContentRecord && from.record() instanceof ContentRecord) {
						next = new ContentChange.Modified(
								makePath(entry.key),
								((ContentRecord) from.record()).toContent(),
								((ContentRecord) to.record()).toContent());
					} else if (from.record() instanceof ContentRecord) {
						next = new ContentChange.Removed(
								makePath(entry.key),
								((ContentRecord) from.record()).toContent());
					} else if (to.record() instanceof ContentRecord) {
						next = new ContentChange.Added(
								makePath(entry.key),
								((ContentRecord) to.record()).toContent());
					}
					
				}
				
				boolean fromIsTree = from != null && from.record() instanceof CollectionRecord;
				boolean toIsTree = to != null && to.record() instanceof CollectionRecord;
				
				if (fromIsTree && toIsTree) {
					push(entry.key, new Level((CollectionRecord) from.record(), (CollectionRecord) to.record()));
				} else if (fromIsTree) {
					push(entry.key, new Level((CollectionRecord) from.record(), null));
				} else if (toIsTree) {
					push(entry.key, new Level(null, (CollectionRecord) to.record()));
				}
				
				restack();
			}
		}
	
		@Override
		public boolean hasNext() {
			findNext();
			return next != null;
		}
	
		@Override
		public ContentChange next() {
			findNext();
			ContentChange e = next;
			next = null;
			return e;
		}
	
		@Override
		public void remove() {
			throw new UnsupportedOperationException("cannot remove with this iterator");
		}
		
		private ObjectIdAndRecord<?> childOrNull(CollectionRecord tree, PathElement key) {
			if (tree != null) {
				ObjectId child = tree.children().get(key);
				if (child != null) {
					return reader.get(child);
				}
			}
			return null;
		}
	
	}
}
