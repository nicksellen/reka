package reka.core.runtime;

import static java.lang.String.format;
import reka.api.run.RouteKey;

public class NodeChild {
    
	public NodeChild(Node node, boolean optional, RouteKey key) {
		this.node = node;
		this.optional = optional;
		this.key = key;
	}
	
	private final Node node;
	private final boolean optional;
	private final RouteKey key;
	
	public Node node() {
		return node;
	}
	
	public boolean optional() {
		return optional;
	}
	
	public RouteKey key() {
		return key;
	}
	
	@Override
	public String toString() {
		return optional ? format("(%s)", node) : format("%s", node);
	}
}