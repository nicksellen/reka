package reka.flow.builder;

import java.util.Map.Entry;

import reka.flow.Flow;
import reka.flow.FlowSegment;
import reka.util.Path;

public class SingleFlow {
	
	public static Entry<Flow,FlowVisualizer> create(Path name, FlowSegment segment) {
		return new FlowBuilderGroup().add(name, segment).build().flowsAndVisualizers().entrySet().iterator().next();
	}

}
