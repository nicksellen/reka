package reka.core.runtime;

import static java.lang.String.format;

public class NodeChild {
    
	public NodeChild(Node node, boolean optional, String label) {
		this.node = node;
		this.optional = optional;
		this.label = label;
	}
	
	private final Node node;
	private final boolean optional;
	private final String label;
	
	public Node node() {
		return node;
	}
	
	public boolean optional() {
		return optional;
	}
	
	public String label() {
		return label;
	}
	
	@Override
	public String toString() {
		return optional ? format("(%s)", node) : format("%s", node);
	}
}