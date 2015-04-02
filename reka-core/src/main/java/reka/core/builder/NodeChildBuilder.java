package reka.core.builder;

import static java.lang.String.format;
import reka.api.run.RouteKey;
import reka.core.runtime.NodeChild;

class NodeChildBuilder {

	static NodeChildBuilder create(NodeBuilder node) {
		return create(node, null);
	}
	
	static NodeChildBuilder create(NodeBuilder node, RouteKey key) {
		return new NodeChildBuilder(node, false, key);
	}
	
	private NodeChildBuilder(NodeBuilder node, boolean optional, RouteKey key) {
		this.node = node;
		this.optional = optional;
		this.key = key;
	}
	
	private final RouteKey key;
	private final NodeBuilder node;
	private boolean optional;
	
	public NodeChild build(NodeFactory factory) {
		return new NodeChild(factory.get(node.id()), optional, key);
	}
	
	public NodeBuilder builder() {
		return node;
	}
	
	public boolean optional() {
		return optional;
	}
	
	public void optional(boolean value) {
		optional = value;
	}
	
	@Override
	public String toString() {
		return optional ? format("(%s)", node) : format("%s", node);
	}
	
}