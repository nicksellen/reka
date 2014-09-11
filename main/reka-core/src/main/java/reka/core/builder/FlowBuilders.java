package reka.core.builder;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static reka.core.builder.FlowConnector.connectSegments;
import static reka.core.builder.FlowSegments.seq;
import static reka.core.builder.FlowSegments.createStartNode;
import static reka.core.builder.FlowSegments.createSubscribeableEndNode;
import static reka.util.Util.runtime;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import reka.api.Path;
import reka.api.flow.Flow;
import reka.api.flow.FlowNode;
import reka.api.flow.FlowSegment;
import reka.core.builder.FlowVisualizer.NodeType;
import reka.core.runtime.DefaultFlow;
import reka.core.runtime.Node;

import com.google.common.collect.ImmutableMap;

public class FlowBuilders {
	
	private final ExecutorService executor = Executors.newCachedThreadPool();
	
	private final Map<Path,FlowInfo> roots = new HashMap<>();
	
	public static Flow createFlow(Path name, FlowSegment segment) {
		FlowBuilders b = new FlowBuilders();
		b.add(name, segment);
		return b.build().flow(name);
	}
	
	public static FlowVisualizer createVisualizer(Path name, FlowSegment segment) {
		FlowBuilders builders = new FlowBuilders();
		builders.add(name, segment);
		return builders.buildVisualizersMaps().get(name);
	}
	
	private static class FlowInfo {
		
		private final Path name;
		private final FlowNode start;
		private final FlowNode end;
		private final FlowSegment segment;
		
		private Flow flow;
		private DefaultFlowVisualizer visualizer;
		
		public FlowInfo(Path name, FlowSegment main) {
			this.name = name;
			this.start = createStartNode("start");
			this.end = createSubscribeableEndNode("end");
			this.segment = seq(start, main, end);;
		}
		
		public FlowInfo visualizer(DefaultFlowVisualizer visualizer) {
			this.visualizer = visualizer;
			return this;
		}
		
		public DefaultFlowVisualizer visualizer() {
			return visualizer;
		}
		
		public FlowInfo flow(Flow flow) {
			this.flow = flow;
			return this;
		}
		
		public Flow flow() {
			return flow;
		}
		
		public Path name() {
			return name;
		}
		
		public FlowNode start() {
			return start;
		}
		
		@SuppressWarnings("unused") // I think this might be useful if I allow subscriptions to multiple points within the flow
		public FlowNode end() {
			return end;
		}
		
		public FlowSegment segment() {
			return segment;
		}
	}
	
	public FlowBuilders add(Path name, FlowSegment segment) {
		checkState(!roots.containsKey(name), "duplicate root names not allows [%s is already registered]", name);
		roots.put(name, new FlowInfo(name, segment));
		return this;
	}
	
	public Collection<Path> roots() {
		return roots.keySet();
	}
	
	public Flows build() {
		
		for (FlowInfo root : roots.values()) {
			createFlow(root, true);
		}
		
		Flows flows = new Flows();
		
		for (FlowInfo info : roots.values()) {
			flows.add(info.flow(), info.visualizer());
		}
		
		return flows;
	}
	
	public Collection<FlowVisualizer> buildVisualizers() {
		return buildVisualizersMaps().values();
	}
	
	private Map<Path,FlowVisualizer> buildVisualizersMaps() {
		
		for (FlowInfo root : roots.values()) {
			createFlow(root, false);
		}
		
		Map<Path,FlowVisualizer> visualizers = new HashMap<>();
		
		for (FlowInfo info : roots.values()) {
			visualizers.put(info.name(), info.visualizer());
		}
		
		return visualizers;
	}
	
	private Map<Path,Flow> makeMapOfBuiltFlows() {
		ImmutableMap.Builder<Path,Flow> builder = ImmutableMap.builder();
		
		for (FlowInfo info : roots.values()) {
			if (info.flow() != null) {
				builder.put(info.name(), info.flow());
			}
		}
		
		return builder.build();
	}
	
	private final Map<Path,FlowConnector> connectors = new HashMap<>();
	
	private void createFlow(FlowInfo info, boolean buildFlow) {
		
		FlowConnector connections = connectSegments(info.segment());
		
		connectors.put(info.name(), connections);
		
		if (buildFlow) {
			for (Path name : connections.usesFlows()) {
				FlowInfo nested = roots.get(name);
				if (nested == null) {
					for (Entry<Path, FlowInfo> root : roots.entrySet()) {
						if (root.getKey().endsWith(name)) {
							nested = root.getValue();
							break;
						}
					}
				}
				checkNotNull(nested, "flow [%s] is required by [%s] but was not defined, we did have %s though", name, info.name(), roots.keySet());
				if (nested.flow() == null) createFlow(nested, buildFlow);
			}
		}
		
		Collection<FlowSegment> segments = connections.segments();
		
	    int nextId = 0;
	    
		Map<Integer,NodeBuilder> idToNodeBuilder = new HashMap<>();
		Map<FlowNode,Integer> flowNodeToId = new HashMap<>();
		Map<Integer,String> idToName = new HashMap<>();
		Map<Integer,NodeType> idToType = new HashMap<>();
		
		for (FlowNode node : connections.nodes()) {
		    int id = nextId++;
		    NodeBuilder builder = new NodeBuilder(id, node.label(), node, executor);
			idToNodeBuilder.put(id, builder);
			flowNodeToId.put(node, id);
			idToName.put(id, builder.name());
			NodeType type = NodeType.NORMAL;
			if (node.isStart()) {
				type = NodeType.START;
			} else if (node.isEnd()) {
				type = NodeType.END;
			} else if (node.hasEmbeddedFlow()) {
				type = NodeType.EMBEDDED;
			}
			idToType.put(id, type);
		}

		for (FlowNodeConnection connection : connections.connections()) {
		    Integer parentId = flowNodeToId.get(connection.source());
		    checkNotNull(parentId);
		    
            NodeBuilder parent = idToNodeBuilder.get(parentId);
            checkNotNull(parent);
		    
		    Integer childId = flowNodeToId.get(connection.destination());
		    checkNotNull(childId);
		    
			NodeBuilder child = idToNodeBuilder.get(childId);
			checkNotNull(child);
			
			parent.addChild(child, connection.name());
			child.incrementParentCount();
		}
		
		NodeBuilder headBuilder = idToNodeBuilder.get(flowNodeToId.get(info.start()));
		
		if (buildFlow) {
			configure(new ConfigurationNodePath(NodeChildBuilder.create(headBuilder)));
			Map<Path,Flow> dependencies = makeMapOfBuiltFlows();
	        NodeFactory factory = new NodeFactory(idToNodeBuilder, dependencies);        
	        Node headNode = factory.get(flowNodeToId.get(info.start()));
	        info.flow(new DefaultFlow(info.name(), headNode));
		}
		
        info.visualizer(new DefaultFlowVisualizer(info.name(), flowNodeToId, idToName, idToType, segments, connections));
	}
	
	private void configure(ConfigurationNodePath path) {
		configure(path, null, null);
	}
	
	private void configure(ConfigurationNodePath path, NodeBuilder trigger, NodeBuilder head) {
		
		NodeChildBuilder current = path.last();
		
		boolean hasMultipleParents = current.node().parentCount() > 1;
		
		if (trigger == null) {
			
			if (hasMultipleParents || current.optional()) {
				current.node().incrementInitialCounter();
			}
			
		} else {
			
			if (hasMultipleParents) {
				current.node().incrementInitialCounter();	
			}
			
			if (hasMultipleParents || current.optional() || current.node().node().isSubscribeable()) {
				
				// handle the trigger and make current the new trigger
				
				trigger.addListener(current.node().id());
				current.node().isTrigger(true);
			}
		}
		
		if (current.optional()) {
			current.node().isTrigger(true);
		}
		
		boolean processChildren = true;
		
		if (current.node().parentCount() > 1) {
			if (current.node().parentCount() != current.node().initialRemainingCount()) {
				processChildren = false;
			}
		}
		
		if (processChildren) {
			
			if (current.node().isTrigger()) {
				trigger = current.node();
			}
		
			for (NodeChildBuilder child : current.node().children()) {
				
				for (NodeChildBuilder c : path.path) {
					if (c.node().equals(child.node())) {
						throw runtime("circular path detected trying to add %s to the end of %s", child.node(), path);
					}
				}
				
				if (current.node().isRouterNode()) {
					child.optional(true);
				}
				
				configure(path.add(child), trigger, head);
			}
		
		}
	}
	
}
