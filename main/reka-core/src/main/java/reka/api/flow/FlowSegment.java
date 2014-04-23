package reka.api.flow;

import java.util.Collection;

public interface FlowSegment {

    // not optional!
    public Collection<FlowSegment> sources();
    public Collection<FlowSegment> destinations();
    public Collection<FlowConnection> connections();
	public Collection<FlowSegment> segments();
	public boolean isNode();
	//public boolean isEmpty();

    // all optional (well, should be)
    public String inputName();
    public String label();
    public String outputName();
	public FlowNode node();
	
}