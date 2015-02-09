package reka.api.flow;

import java.util.Collection;

import reka.api.data.Data;
import reka.api.run.RouteKey;

public class FlowSegmentProxy implements FlowSegment {
	
	private final FlowSegment parent;
	
	public FlowSegmentProxy(FlowSegment parent) {
		this.parent = parent;
	}

	@Override
	public Collection<FlowSegment> sources() {
		return parent.sources();
	}

	@Override
	public Collection<FlowSegment> destinations() {
		return parent.destinations();
	}

	@Override
	public Collection<FlowConnection> connections() {
		return parent.connections();
	}

	@Override
	public Collection<FlowSegment> segments() {
		return parent.segments();
	}

	@Override
	public boolean isNode() {
		return parent.isNode();
	}
	
	@Override
	public Data meta() {
		return parent.meta();
	}

	@Override
	public RouteKey key() {
		return parent.key();
	}

	@Override
	public String label() {
		return parent.label();
	}

	@Override
	public FlowNode node() {
		return parent.node();
	}

	@Override
	public boolean isNewContext() {
		return parent.isNewContext();
	}

}
