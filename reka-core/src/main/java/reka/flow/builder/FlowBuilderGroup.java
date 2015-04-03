package reka.flow.builder;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static reka.flow.builder.FlowConnector.connectionsFor;
import static reka.flow.builder.FlowSegments.createStartNode;
import static reka.flow.builder.FlowSegments.createSubscribeableEndNode;
import static reka.flow.builder.FlowSegments.seq;
import static reka.util.Util.runtime;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.Reka;
import reka.flow.Flow;
import reka.flow.FlowNode;
import reka.flow.FlowSegment;
import reka.flow.builder.FlowVisualizer.NodeType;
import reka.runtime.DefaultFlow;
import reka.runtime.Node;
import reka.util.Path;

import com.google.common.collect.ImmutableMap;

public class FlowBuilderGroup {
	
	@SuppressWarnings("unused")
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	private final Map<Path,FlowInfo> roots = new HashMap<>();
	
	public static Flow createFlow(Path name, FlowSegment segment) {
		FlowBuilderGroup b = new FlowBuilderGroup();
		b.add(name, segment);
		return b.build().flow(name);
	}
	
	public static FlowVisualizer createVisualizer(Path name, FlowSegment segment) {
		FlowBuilderGroup builders = new FlowBuilderGroup();
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
			checkState(!built(), "already built!");
			this.visualizer = visualizer;
			return this;
		}
		
		public DefaultFlowVisualizer visualizer() {
			return visualizer;
		}
		
		public FlowInfo flow(Flow flow) {
			checkState(!built(), "already built!");
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
		
		public boolean built() {
			return flow != null && visualizer != null;
		}
		
	}
	
	public FlowBuilderGroup add(Path name, FlowSegment segment) {
		checkState(!roots.containsKey(name), "duplicate root names not allows [%s is already registered]", name);
		roots.put(name, new FlowInfo(name, segment));
		return this;
	}
	
	public Collection<Path> roots() {
		return roots.keySet();
	}
	
	public Flows build() {
		
		for (FlowInfo root : new ArrayList<>(roots.values())) {
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
		if (info.built()) return;
		
		FlowConnector connections = connectionsFor(info.name(), info.segment());
		
		for (Entry<Path, FlowSegment> e : connections.newContextSegments().entrySet()) {
			add(e.getKey(), e.getValue());
		}
		
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
		Map<FlowNode,Integer> nodeToId = new HashMap<>();
		Map<Integer,String> idToName = new HashMap<>();
		Map<Integer,NodeType> idToType = new HashMap<>();
		
		for (FlowNode node : connections.nodes()) {
		    int id = nextId++;
		    NodeBuilder builder = new NodeBuilder(id, node.label(), node, Reka.SharedExecutors.general);
			idToNodeBuilder.put(id, builder);
			nodeToId.put(node, id);
			idToName.put(id, builder.name());
			NodeType type = NodeType.NORMAL;
			if (node.isStart()) {
				type = NodeType.START;
			} else if (node.isEnd()) {
				type = NodeType.END;
			} else if (node.hasFlowReference()) {
				type = NodeType.REFERENCE;
			}
			idToType.put(id, type);
		}

		for (FlowNodeConnection connection : connections.connections()) {
		    Integer parentId = nodeToId.get(connection.source());
		    checkNotNull(parentId);
		    
            NodeBuilder parent = idToNodeBuilder.get(parentId);
            checkNotNull(parent);
		    
		    Integer childId = nodeToId.get(connection.destination());
		    checkNotNull(childId);
		    
			NodeBuilder child = idToNodeBuilder.get(childId);
			checkNotNull(child);
			
			parent.addChild(child, connection.key());
			child.incrementParentCount();
		}
		
		NodeBuilder headBuilder = idToNodeBuilder.get(nodeToId.get(info.start()));
		
		if (buildFlow) {
			configure(new ConfigurationNodePath(NodeChildBuilder.create(headBuilder)));
			Map<Path,Flow> dependencies = makeMapOfBuiltFlows();
	        NodeFactory factory = new NodeFactory(idToNodeBuilder, dependencies);        
	        Node headNode = factory.get(nodeToId.get(info.start()));
	        info.flow(new DefaultFlow(info.name(), headNode));
	        //System.out.printf("flow action dot for [%s]:\n\n%s\n", info.name.slashes(), factory.toDot());
		}
		
        info.visualizer(new DefaultFlowVisualizer(info.name(), nodeToId, idToName, idToType, segments, connections));
	}
	
	private void configure(ConfigurationNodePath path) {
		configure(path, null, null);
	}
	
	private void configure(ConfigurationNodePath path, NodeBuilder trigger, NodeBuilder head) {
		
		NodeChildBuilder current = path.last();
		
		boolean hasMultipleParents = current.builder().parentCount() > 1;
		
		if (hasMultipleParents || 
			(trigger == null && current.optional())) {
			// a node that needs to wait for previous nodes to complete we store how many it will wait for
			current.builder().incrementInitialCounter();
		}
		
		if (current.optional() || 
			current.builder().node().hasFlowReference() || 
			current.builder().node().isEnd() ||
			(trigger != null && hasMultipleParents)) {
			// a node that needs to notify downstream nodes when it is halted
			current.builder().isTrigger(true);
		}
		
		if (trigger != null && current.builder().isTrigger()) {
			trigger.addListener(current.builder().id());
		}
		
		boolean processChildren = !hasMultipleParents || current.builder().parentCount() == current.builder().initialCounter();
		
		if (processChildren) {
			
			if (current.builder().isTrigger()) {
				trigger = current.builder();
			}
		
			for (NodeChildBuilder child : current.builder().children()) {
				
				for (NodeChildBuilder c : path.path) {
					if (c.builder().equals(child.builder())) {
						throw runtime("circular path detected trying to add %s to the end of %s", child.builder(), path);
					}
				}
				
				if (current.builder().node().isRouter()) {
					child.optional(true);
				}
				
				configure(path.add(child), trigger, head);
			}
		
		}
		
	}
	
}
