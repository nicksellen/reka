package reka.gitfordata.tree.traversal;

import reka.api.Path;

public class ListPathEvaluator implements PathEvaluator {

	private final Path path;
	
	public ListPathEvaluator(Path path) {
		this.path = path;
	}
	
	@Override
	public Evaluation evaluate(Path incoming) {
		return new DefaultEvaluation()
			.proceed(path.startsWith(incoming) || (incoming.startsWith(path) && (incoming.length() < path.length() + 1)))
			.include(incoming.length() == path.length() + 1);
	}

}
