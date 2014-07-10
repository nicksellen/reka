package reka.core.builder;

import reka.api.flow.FlowNode;

public class FlowNodeConnection {
    
	private final FlowNode source;
	private final FlowNode destination;
	private final String label;
	private final boolean optional;
	
	private FlowNodeConnection(FlowNode source, FlowNode destination, String alias, boolean optional) {
		this.source = source;
		this.destination = destination;
		this.label = alias;
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
	
	public String label() {
		return label;
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
		result = prime * result + ((label == null) ? 0 : label.hashCode());
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
		if (label == null) {
			if (other.label != null)
				return false;
		} else if (!label.equals(other.label))
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