package reka.flow;

import java.util.Collection;

import reka.data.Data;
import reka.flow.ops.RouteKey;

public interface FlowSegment {

    // not optional!
	
    Collection<FlowSegment> sources();
    Collection<FlowSegment> destinations();
    Collection<FlowConnection> connections();
	Collection<FlowSegment> segments();
	
	boolean isNode();
	
	default boolean isNewContext() {
		return false;
	}
	
	Data meta();

    // all optional (well, should be)
	
    RouteKey key();
    String label();
	FlowNode node();
	
	default FlowSegment withNewContext() {
		return new FlowSegmentProxy(this){

			@Override
			public boolean isNewContext() {
				return true;
			}
			
		};
	}
	
	default FlowSegment clearNewContext() {
		return new FlowSegmentProxy(this){

			@Override
			public boolean isNewContext() {
				return false;
			}
			
		};
	}
	
}