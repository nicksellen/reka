package reka.flow.builder;

import java.util.Map.Entry;

import reka.api.Path;
import reka.flow.Flow;
import reka.flow.FlowSegment;

public class SingleFlow {
	
	public static Entry<Flow,FlowVisualizer> create(Path name, FlowSegment segment) {
		return new FlowBuilderGroup().add(name, segment).build().flowsAndVisualizers().entrySet().iterator().next();
	}

}
