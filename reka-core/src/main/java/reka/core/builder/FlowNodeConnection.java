package reka.core.builder;

import static java.lang.String.format;
import reka.api.flow.FlowNode;
import reka.api.run.RouteKey;

public class FlowNodeConnection {
    
	private final FlowNode source;
	private final FlowNode destination;
	private final RouteKey key;
	private final boolean optional;
	
	private FlowNodeConnection(FlowNode source, FlowNode destination, RouteKey key, boolean optional) {
		this.source = source;
		this.destination = destination;
		this.key = key;
		this.optional = optional;
	}
	
	public static FlowNodeConnection create(FlowNode source, FlowNode destination, RouteKey key, boolean optional) {
		return new FlowNodeConnection(source, destination, key, optional);
	}
	
	public FlowNode source() {
		return source;
	}
	
	public FlowNode destination() {
		return destination;
	}
	
	public RouteKey key() {
		return key;
	}
	
	public boolean optional() {
		return optional;
	}
	
	@Override
	public String toString() {
		return format("FlowNodeConnection(source=%s, destination=%s, key=%s, optional=%s", source, destination, key, optional);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((destination == null) ? 0 : destination.hashCode());
		result = prime * result + ((key == null) ? 0 : key.hashCode());
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
		if (key == null) {
			if (other.key != null)
				return false;
		} else if (!key.equals(other.key))
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