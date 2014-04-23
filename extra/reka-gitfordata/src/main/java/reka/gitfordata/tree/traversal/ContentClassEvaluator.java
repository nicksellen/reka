package reka.gitfordata.tree.traversal;

import reka.api.content.Content;

public abstract class ContentClassEvaluator implements ContentEvaluator {

	private final Class<?> klass;
	
	public ContentClassEvaluator(Class<?> klass) {
		this.klass = klass;
	}
	
	@Override
	public boolean evaluate(Content content) {
		return klass.isInstance(content);
	}

}
