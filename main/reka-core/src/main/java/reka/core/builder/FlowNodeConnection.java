package reka.core.builder;

import java.util.Objects;

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
		return Objects.hash(source, destination, label);
	}
	
	@Override
	public boolean equals(Object object) {
		if (object == this) return true;
		if (!(object instanceof FlowNodeConnection)) return false;
		FlowNodeConnection other = (FlowNodeConnection) object;
		return source.equals(other.source)
			&& destination.equals(other.destination)
			&& ((label == null && other.label == null) || label.equals(other.label));
	}
}