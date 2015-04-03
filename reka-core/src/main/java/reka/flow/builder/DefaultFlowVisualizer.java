package reka.flow.builder;

import static reka.util.Util.createEntry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import reka.data.Data;
import reka.flow.FlowNode;
import reka.flow.FlowSegment;
import reka.util.Path;

public class DefaultFlowVisualizer implements FlowVisualizer {

	private final Path name;
	
	private final Map<Integer,String> nodeIdToName;
	private final Collection<FlowSegment> segments;

	private final FlowConnector connections;
	
	private final Map<FlowSegment,FlowSegment> labelParents = new HashMap<>();
	private final Map<FlowNode,FlowSegment> nodeParents = new HashMap<>();

	private final Map<FlowSegment,FlowSegment> metaParents = new HashMap<>();
	private final Map<FlowNode,FlowSegment> nodeMetaParents = new HashMap<>();
	
	private final List<Entry<Path,Integer>> labelPaths;
	private final Map<FlowNode, Integer> nodeToId;
	private final Map<Integer,NodeType> idToType;

	private final Map<Integer,List<Data>> metaStacks;
	
	public DefaultFlowVisualizer(
			Path name, 
			Map<FlowNode,Integer> nodeToId, 
			Map<Integer,String> nodeIdToNames, 
			Map<Integer,NodeType> idToType,
			Collection<FlowSegment> segments,
			FlowConnector connections) {
		this.name = name;
		this.nodeToId = nodeToId;
		this.nodeIdToName = nodeIdToNames;
		this.idToType = idToType;
		this.segments = segments;
        this.connections = connections;
		walkForLabels();
		walkForMeta();
        labelPaths = extractLabelPaths();
        metaStacks = extractSomethingAboutMeta();
	}
	
	@Override
	public Path name() {
		return name;
	}
	
	private void walkForLabels() {
		for (FlowSegment root : segments) {
			walkForLabels(root, null);
		}
	}
	
	private void walkForMeta() {
		for (FlowSegment root : segments) {
			walkForMeta(root, null);
		}
	}
	
	private void walkForLabels(FlowSegment segment, FlowSegment parent) {
		
		if (parent != null) {
			if (segment.isNode()) {
				nodeParents.put(segment.node(), parent);
			} else {
				if (segment != parent && segment.label() != null && parent.label() != null) {
					labelParents.put(segment, parent);
				}
			}
		}
		
		if (segment.label() != null && !segment.isNode()) parent = segment;
		
		for (FlowSegment subsegment : segment.segments()) {
			if (subsegment != segment && subsegment != null) {
				walkForLabels(subsegment, parent);
			}
		}
	}
	
	private void walkForMeta(FlowSegment segment, FlowSegment parent) {
		
		if (parent != null) {
			if (segment.isNode()) {
				nodeMetaParents.put(segment.node(), parent);
			} else {
				if (segment != parent && segment.meta().size() > 0) {
					metaParents.put(segment, parent);
				}
			}
		}
		
		if (segment.meta().size() > 0 && !segment.isNode()) parent = segment;
		
		for (FlowSegment subsegment : segment.segments()) {
			if (subsegment != segment && subsegment != null) {
				walkForMeta(subsegment, parent);
			}
		}
	}
	
	private Map<Integer,List<Data>> extractSomethingAboutMeta() {
		
		Map<Integer,List<Data>> items = new HashMap<>();
		
		for (Entry<FlowNode, FlowSegment> entry : nodeMetaParents.entrySet()) {
			
			FlowNode flowNode = entry.getKey();
			if (flowNode.isNoOp()) continue;

			int id = nodeToId.get(flowNode);
			if (!nodeIdToName.keySet().contains(id)) continue;
			
			FlowSegment to = entry.getValue();
			
			FlowSegment segment = to;
			
			List<Data> metaStack = new ArrayList<>();
			
			if (flowNode.meta().size() > 0) {
				metaStack.add(flowNode.meta());
			}
			
			while (segment != null) {
				if (segment.meta().size() > 0) {
					metaStack.add(segment.meta());
				}
				segment = metaParents.get(segment);
			}
			
			if (!metaStack.isEmpty()) {
				items.put(id, metaStack);
			}
		}
		
		return items;
	}
	

	private List<Entry<Path,Integer>> extractLabelPaths() {
		
		List<Entry<Path,Integer>> items = new ArrayList<>();
		for (Entry<FlowNode, FlowSegment> entry : nodeParents.entrySet()) {
			
			FlowNode flowNode = entry.getKey();
			
			if (flowNode.isNoOp()) continue;

			int id = nodeToId.get(flowNode);
			if (!nodeIdToName.keySet().contains(id)) continue;
			
			FlowSegment to = entry.getValue();
			
			FlowSegment segment = to;
			Path.Builder builder = Path.newBuilder();
			while (segment != null) {
				builder.add(segment.label());
				segment = labelParents.get(segment);
			}
			
			Path path = builder.build().reverse();
			
			items.add(createEntry(path, id));
		}
		
		return items;
	}
	
	@Override
	public <T> T build(GraphVisualizer<T> graph) {
		
		for (Entry<Path, Integer> labelled : labelPaths) {
	        graph.group(labelled.getKey(), labelled.getValue());
	    }
		
		for (Entry<Integer,List<Data>> e : metaStacks.entrySet()) {
			graph.meta(e.getKey(), e.getValue());
		}
	    
	    for (Entry<Integer, String> node : nodeIdToName.entrySet()) {
	        graph.node(node.getKey(), node.getValue(), idToType.get(node.getKey()));
	    }
	    
	    for (FlowNodeConnection connection : connections.connections()) {
	    	graph.connect(nodeToId.get(connection.source()), 
	    			      nodeToId.get(connection.destination()), 
	    			      connection.key() != null ? connection.key().name() : null, 
	    			      connection.optional());
	    }
	    
	    return graph.build();
	}

}
