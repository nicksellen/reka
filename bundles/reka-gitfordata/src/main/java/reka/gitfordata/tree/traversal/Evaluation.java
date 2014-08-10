package reka.gitfordata.tree.traversal;

import reka.api.Path;

public interface Evaluation {
	public boolean proceed();
	public boolean include();
	public Path path();
}
