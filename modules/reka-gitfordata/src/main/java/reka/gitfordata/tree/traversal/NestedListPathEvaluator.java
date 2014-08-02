package reka.gitfordata.tree.traversal;

import reka.api.Path;

public class NestedListPathEvaluator implements PathEvaluator {

	private final Path subpath;
	
	public NestedListPathEvaluator(Path subpath) {
		this.subpath = subpath;
	}
	
	@Override
	public Evaluation evaluate(Path path) {
		return new DefaultEvaluation().include(path.startsWith(subpath)).proceed(path.startsWith(subpath) || subpath.startsWith(path));
	}

}
