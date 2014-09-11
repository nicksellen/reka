package reka.core.runtime;

import static java.lang.String.format;

public class NodeChild {
    
	public NodeChild(Node node, boolean optional, String name) {
		this.node = node;
		this.optional = optional;
		this.name = name;
	}
	
	private final Node node;
	private final boolean optional;
	private final String name;
	
	public Node node() {
		return node;
	}
	
	public boolean optional() {
		return optional;
	}
	
	public String name() {
		return name;
	}
	
	@Override
	public String toString() {
		return optional ? format("(%s)", node) : format("%s", node);
	}
}