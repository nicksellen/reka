package reka.core.builder;

import reka.api.flow.FlowNode;

public class FlowNodeConnection {
    
	private final FlowNode source;
	private final FlowNode destination;
	private final String name;
	private final boolean optional;
	
	private FlowNodeConnection(FlowNode source, FlowNode destination, String name, boolean optional) {
		this.source = source;
		this.destination = destination;
		this.name = name;
		this.optional = optional;
	}
	
	public static FlowNodeConnection create(FlowNode source, FlowNode destination, String alias, boolean optional) {
		return new FlowNodeConnection(source, destination, alias, optional);
	}
	
	public FlowNode source() {
		return source;
	}
	
	public FlowNode destination() {
		return destination;
	}
	
	public String name() {
		return name;
	}
	
	public boolean optional() {
		return optional;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((destination == null) ? 0 : destination.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + (optional ? 1231 : 1237);
		result = prime * result + ((source == null) ? 0 : source.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		FlowNodeConnection other = (FlowNodeConnection) obj;
		if (destination == null) {
			if (other.destination != null)
				return false;
		} else if (!destination.equals(other.destination))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (optional != other.optional)
			return false;
		if (source == null) {
			if (other.source != null)
				return false;
		} else if (!source.equals(other.source))
			return false;
		return true;
	}
	
}