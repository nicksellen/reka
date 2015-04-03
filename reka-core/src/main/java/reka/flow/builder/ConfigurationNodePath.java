package reka.flow.builder;

import java.util.List;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;

class ConfigurationNodePath {
	final List<NodeChildBuilder> path;
	ConfigurationNodePath() {
		path = ImmutableList.of();
	}
	ConfigurationNodePath(NodeChildBuilder o) {
		path = ImmutableList.of(o);
	}
	ConfigurationNodePath(List<NodeChildBuilder> path) {
		this.path = path;
	}
	ConfigurationNodePath add(NodeChildBuilder o) {
		return new ConfigurationNodePath(ImmutableList.<NodeChildBuilder>builder().addAll(path).add(o).build());
	}
	boolean isEmpty() {
		return path.isEmpty();
	}
	NodeChildBuilder last() {
		return path.isEmpty() ? null : path.get(path.size() - 1);
	}
	public String toString() {
		return Joiner.on(" > ").join(path);
	}
}