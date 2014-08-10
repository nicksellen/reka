package reka.gitfordata.tree.traversal;

import java.util.ArrayList;
import java.util.List;

import reka.api.ObjectBuilder;
import reka.api.Path;
import reka.api.content.Content;

public class TraversalDescription {

	public static TraversalDescription all() {
		return newBuilder().build();
	}
	
	public static TraversalDescription within(Path path, boolean removeSubpath) {
		return newBuilder().within(path, removeSubpath).build();
	}
	
	public static TraversalDescription list(Path path) {
		return newBuilder().list(path).build();
	}
	
	public static TraversalDescription contentType(Class<? extends Content> contentClass) {
		return newBuilder().contentType(contentClass).build();
	}

	public static Builder newBuilder() {
		return new Builder();
	}

	private final List<ContentEvaluator> contentEvaluators;
	private final List<PathEvaluator> pathEvaluators;
	private final int toDepth;
	private final boolean includeAllKeys;

	private TraversalDescription(List<ContentEvaluator> contentEvaluators,
			List<PathEvaluator> pathEvaluators, int toDepth, boolean includeChildKeys) {
		this.contentEvaluators = contentEvaluators;
		this.pathEvaluators = pathEvaluators;
		this.toDepth = toDepth;
		this.includeAllKeys = includeChildKeys;
	}

	public static class Builder implements ObjectBuilder<TraversalDescription> {

		private List<ContentEvaluator> contentEvaluators = new ArrayList<>();
		private List<PathEvaluator> pathEvaluators = new ArrayList<>();
		private int toDepth;
		private boolean includeAllKeys = false;

		private Builder() {
			// use newBuilder static method
		}
		
		public Builder within(Path path, boolean removeSubpath) {
			return addPathEvaluator(new WithinPathEvaluator(path, removeSubpath));
		}
		
		public Builder list(Path path) {
			return addPathEvaluator(new ListPathEvaluator(path));
		}
		
		public Builder contentType(Class<? extends Content> klass) {
			return addContentEvaluator(new ContentClassEvaluator(klass) {});
		}

		public Builder addPathEvaluator(PathEvaluator pathEvaluator) {
			pathEvaluators.add(pathEvaluator);
			return this;
		}

		public Builder addContentEvaluator(ContentEvaluator contentEvaluator) {
			contentEvaluators.add(contentEvaluator);
			return this;
		}

		public Builder toDepth(int value) {
			toDepth = value;
			return this;
		}
		
		public Builder includeAllKeys(boolean value) {
			includeAllKeys = value;
			return this;
		}

		@Override
		public TraversalDescription build() {
			return new TraversalDescription(contentEvaluators, pathEvaluators,
					toDepth, includeAllKeys);
		}

	}

	public List<ContentEvaluator> contentEvaluators() {
		return contentEvaluators;
	}

	public List<PathEvaluator> pathEvaluators() {
		return pathEvaluators;
	}

	public int toDepth() {
		return toDepth;
	}
	
	public boolean includeAllKeys() {
		return includeAllKeys;
	}

}
