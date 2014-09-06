package reka.core.builder;

import java.util.Map;
import java.util.Map.Entry;

import reka.api.IdentityStore;
import reka.api.Path;
import reka.api.flow.Flow;
import reka.api.flow.FlowSegment;

public class SingleFlow {
	
	public static Entry<Flow,FlowVisualizer> create(Path name, FlowSegment segment, Map<Integer,IdentityStore> stores) {
		return new FlowBuilders().add(name, segment).build(stores).flowsAndVisualizers().entrySet().iterator().next();
	}

}
