package reka.core.builder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import reka.api.Path;
import reka.api.flow.Flow;

public class Flows {
	
	private final Map<Flow,FlowVisualizer> flows = new HashMap<>();
	private final Map<Path,Flow> flowsByName = new HashMap<>();
	private final Map<Path,FlowVisualizer> visualizersByName = new HashMap<>();
	
	private final List<Path> names = new ArrayList<>();

	public void add(Flow flow, FlowVisualizer visualizer) {
		flows.put(flow, visualizer);
		flowsByName.put(flow.name(), flow);
		visualizersByName.put(flow.name(), visualizer);
		names.add(flow.name());
	}
	
	public Collection<Flow> flows() {
		return flows.keySet();
	}
	
	public Map<Flow,FlowVisualizer> flowsAndVisualizers() {
		return flows;
	}
	
	public Flow flow(Path name) {
		Flow flow = flowsByName.get(name);
		
		if (flow == null) {
			for (Entry<Path, Flow> e : flowsByName.entrySet()) {
				if (e.getKey().endsWith(name)) {
					flow = e.getValue();
					break;
				}
			}
		}
		
		return flow;
	}
	
	public FlowVisualizer visualizer(Path name) {
		FlowVisualizer vis = visualizersByName.get(name);
		
		if (vis == null) {
			for (Entry<Path, FlowVisualizer> e : visualizersByName.entrySet()) {
				if (e.getKey().endsWith(name)) {
					vis = e.getValue();
					break;
				}
			}
		}
		
		return vis;
	}
	
	public Collection<Path> names() {
		return names;
	}

}
