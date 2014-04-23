package reka.gitfordata.tree.traversal;

import reka.api.Path;

public class DefaultEvaluation implements Evaluation {
	
	private boolean proceed = true;
	private boolean include = true;
	private Path path;
	
	public DefaultEvaluation proceed(boolean value) {
		proceed = value;
		return this;
	}
	
	public DefaultEvaluation include(boolean value) {
		include = value;
		return this;
	}
	
	public DefaultEvaluation path(Path value) {
		path = value;
		return this;
	}

	@Override
	public boolean proceed() {
		return proceed;
	}

	@Override
	public boolean include() {
		return include;
	}

	@Override
	public Path path() {
		return path;
	}

}
