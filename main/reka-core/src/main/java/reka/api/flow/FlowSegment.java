package reka.api.flow;

import java.util.Collection;

import reka.api.data.Data;

public interface FlowSegment {

    // not optional!
    Collection<FlowSegment> sources();
    Collection<FlowSegment> destinations();
    Collection<FlowConnection> connections();
	Collection<FlowSegment> segments();
	boolean isNode();
	//boolean isEmpty();

    // all optional (well, should be)
    String inputName();
    String label();
    String outputName();
	FlowNode node();
	
	Data meta();
	
}