package reka.api.flow;

import java.util.Collection;

import reka.api.data.Data;
import reka.api.run.RouteKey;

public interface FlowSegment {

    // not optional!
	
    Collection<FlowSegment> sources();
    Collection<FlowSegment> destinations();
    Collection<FlowConnection> connections();
	Collection<FlowSegment> segments();
	
	boolean isNode();
	
	Data meta();

    // all optional (well, should be)
	
    RouteKey key();
    String label();
	FlowNode node();
	
}