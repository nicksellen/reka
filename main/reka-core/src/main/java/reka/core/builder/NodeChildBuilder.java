package reka.core.builder;

import static java.lang.String.format;
import reka.core.runtime.NodeChild;

class NodeChildBuilder {

	static NodeChildBuilder create(NodeBuilder node) {
		return create(node, null);
	}
	
	static NodeChildBuilder create(NodeBuilder node, String alias) {
		return new NodeChildBuilder(node, false, alias);
	}
	
	private NodeChildBuilder(NodeBuilder node, boolean optional, String alias) {
		this.node = node;
		this.optional = optional;
		this.alias = alias;
	}
	
	private final String alias;
	private final NodeBuilder node;
	private boolean optional;
	
	public NodeChild build(NodeFactory factory) {
		return new NodeChild(factory.get(node.id()), optional, alias);
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
	
	public String alias() {
		return alias;
	}
	
	@Override
	public String toString() {
		return optional ? format("(%s)", node) : format("%s", node);
	}
	
}