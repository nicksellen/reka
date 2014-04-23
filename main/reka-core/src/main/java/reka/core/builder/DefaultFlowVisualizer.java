package reka.core.builder;

import static reka.util.Util.createEntry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import reka.api.Path;
import reka.api.flow.FlowNode;
import reka.api.flow.FlowSegment;

public class DefaultFlowVisualizer implements FlowVisualizer {

	private final Path name;
	
	private final Map<Integer,String> nodes;
	private final Collection<FlowSegment> segments;

	private final FlowConnector connections;
	
	private final Map<FlowSegment,FlowSegment> labelParents = new HashMap<>();
	private final Map<FlowNode,FlowSegment> nodeParents = new HashMap<>();
	
	private final List<Entry<Path,Integer>> labelPaths;
	private final Map<FlowNode, Integer> nodeToId;
	private final Map<Integer,NodeType> idToType;
	
	public DefaultFlowVisualizer(
			Path name, 
			Map<FlowNode,Integer> nodeToId, 
			Map<Integer,String> nodes, 
			Map<Integer,NodeType> idToType,
			Collection<FlowSegment> segments,
			FlowConnector connections) {
		
		this.name = name;
		this.nodeToId = nodeToId;
		this.nodes = nodes;
		this.idToType = idToType;
		this.segments = segments;
		walkForLabels();
        labelPaths = extractLabelPaths(nodeToId, nodes.keySet());
        this.connections = connections;
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
	

	private List<Entry<Path,Integer>> extractLabelPaths(Map<FlowNode,Integer> nodeToId, Collection<Integer> nodeIds) {
		
		List<Entry<Path,Integer>> items = new ArrayList<>();
		for (Entry<FlowNode, FlowSegment> entry : nodeParents.entrySet()) {
			
			FlowNode flowNode = entry.getKey();
			FlowSegment to = entry.getValue();
			
			FlowSegment segment = to;
			Path.Builder builder = Path.newBuilder();
			while (segment != null) {
				builder.add(segment.label());
				segment = labelParents.get(segment);
			}
			
			Path path = builder.build().reverse();
			
			int id = nodeToId.get(flowNode);
			
			boolean include = false;
			if (nodeIds.contains(id)) {
				include = true;
			}
			
			if (include) {
			    items.add(createEntry(path, id));
			}
		}
		
		return items;
	}
	
	@Override
	public <T> T build(GraphVisualizer<T> graph) {
		
		for (Entry<Path, Integer> labelled : labelPaths) {
	        graph.group(labelled.getKey(), labelled.getValue());
	    }
	    
	    for (Entry<Integer, String> node : nodes.entrySet()) {
	        graph.node(node.getKey(), node.getValue(), idToType.get(node.getKey()));
	    }
	    
	    for (FlowNodeConnection connection : connections.connections()) {
	    	graph.connect(nodeToId.get(connection.source()), 
	    			      nodeToId.get(connection.destination()), 
	    			      connection.label(), 
	    			      connection.optional());
	    }
	    
	    return graph.build();
	}

	/*
	public static class JsPlumbGraphBuilder implements GraphBuilder<String> {
		
		private final StringBuilder nodes = new StringBuilder();
		private final StringBuilder connections = new StringBuilder();

		@Override
		public void node(int id, String name) {
			nodes.append(format("<div id=\"%s\" class=\"box\">%s</div>\n", sanitize(name), name));
		}
		
		private String sanitize(String value) {
			return value.replaceAll("[^a-zA-Z0-9_]", "_");
		}

		@Override
		public void group(Path path, int id) { }

		@Override
		public void connect(int from, int to, String label, boolean optional) {
			connections.append("jsPlumb.connect({ source: '");
			connections.append(sanitize(from));
			connections.append("', target: '");
			connections.append(sanitize(to)).append("'");
			connections.append(", connector: [ 'Flowchart', {} ]");
			connections.append(" });\n");
		}

		@Override
		public String build() {
			StringBuilder result = new StringBuilder();
			result.append(connections.toString());
			result.append(nodes.toString());
			return result.toString();
		}
		
	}
	*/

}
