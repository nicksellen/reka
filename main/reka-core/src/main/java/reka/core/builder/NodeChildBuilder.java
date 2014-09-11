package reka.core.builder;

import static java.lang.String.format;
import reka.core.runtime.NodeChild;

class NodeChildBuilder {

	static NodeChildBuilder create(NodeBuilder node) {
		return create(node, null);
	}
	
	static NodeChildBuilder create(NodeBuilder node, String name) {
		return new NodeChildBuilder(node, false, name);
	}
	
	private NodeChildBuilder(NodeBuilder node, boolean optional, String name) {
		this.node = node;
		this.optional = optional;
		this.name = name;
	}
	
	private final String name;
	private final NodeBuilder node;
	private boolean optional;
	
	public NodeChild build(NodeFactory factory) {
		return new NodeChild(factory.get(node.id()), optional, name);
	}
	
	public NodeBuilder node() {
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