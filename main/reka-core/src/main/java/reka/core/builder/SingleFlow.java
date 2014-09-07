package reka.core.builder;

import java.util.Map.Entry;

import reka.api.Path;
import reka.api.flow.Flow;
import reka.api.flow.FlowSegment;

public class SingleFlow {
	
	public static Entry<Flow,FlowVisualizer> create(Path name, FlowSegment segment) {
		return new FlowBuilders().add(name, segment).build().flowsAndVisualizers().entrySet().iterator().next();
	}

}
