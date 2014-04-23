package reka.gitfordata.tree.traversal;

import reka.api.Path;

public class WithinPathEvaluator implements PathEvaluator {

	private final Path subpath;
	private final boolean removeSubpath;

	public WithinPathEvaluator(Path subpath, boolean removeSubpath) {
		this.subpath = subpath;
		this.removeSubpath = removeSubpath;
	}

	@Override
	public Evaluation evaluate(Path path) {
		boolean include = path.startsWith(subpath);
		return new DefaultEvaluation()
				.include(include)
				.path(removeSubpath ? (include ? path.subpath(subpath.length()) : null) : path)
				.proceed(include || subpath.startsWith(path));
	}

}
