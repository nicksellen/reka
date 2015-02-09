package reka.core.builder;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static reka.core.builder.FlowSegments.noop;
import static reka.util.Util.createEntry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.api.Path;
import reka.api.flow.FlowConnection;
import reka.api.flow.FlowNode;
import reka.api.flow.FlowReference;
import reka.api.flow.FlowSegment;
import reka.api.run.RouteKey;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.SetMultimap;

public class FlowConnector {

	public static FlowConnector connectionsFor(Path flowName, FlowSegment segment) {
		return new FlowConnector(flowName, segment);
	}

	private final static Logger log = LoggerFactory.getLogger("flow-connections");
	
	private final Path flowName;
	private final FlowSegment rootSegment;
	
	private final Set<FlowSegment> segments = new HashSet<>();
	
	private final Set<FlowNodeConnection> conns = new HashSet<>();
	private final Set<FlowNode> nodes = new HashSet<>();
	private final SetMultimap<FlowNode,FlowNode> destinationsOf = HashMultimap.create();
	private final SetMultimap<FlowNode,FlowNode> sourcesOf = HashMultimap.create();

	private final Map<Path,FlowSegment> newContextSegments = new HashMap<>();
	
	private final List<FlowNodeConnection> noOpConnections = new ArrayList<>();
	
	private final Set<Path> usesFlows = new HashSet<>();
	
	private FlowConnector(Path flowName, FlowSegment segment) {
		this.flowName = flowName;
		this.rootSegment = segment;
		expand(segment);
		fixupNoOps();
	}
	
	public Path flowName() {
		return flowName;
	}

	public Map<Path,FlowSegment> childContextSegments() {
		return newContextSegments;
	}

	public Collection<FlowNodeConnection> connections() {
		return conns;
	} 
	
	public Collection<Path> usesFlows() {
		return usesFlows;
	}

	public Collection<FlowNode> nodes() {
		return ImmutableList.copyOf(nodes);
	}
	
	public Collection<FlowSegment> segments() {
		return ImmutableList.copyOf(segments);
	}
	
	public Collection<FlowNode> heads() {
		Set<FlowNode> results = new HashSet<>();
		for (FlowNode node : nodes) {
			if (sourcesOf.get(node).isEmpty()) {
				results.add(node);
			}
		}
		return results;
	}

	public Collection<FlowNode> tails() {
		Set<FlowNode> results = new HashSet<>();
		for (FlowNode node : nodes) {
			log.debug("checking destinations of [{}]", node.label());
			if (destinationsOf.get(node).isEmpty()) {
				log.debug("  none!");
				results.add(node);
			} else {
				log.debug("  some! {}", destinationsOf.get(node));
			}
		}
		return results;
	}

	public Collection<FlowNode> tailsOf(FlowNode node) {
		return tailsOf(node, new HashSet<FlowNode>());
	}
	
	private void fixupNoOps() {
		for (FlowNodeConnection connection : noOpConnections) {
			if (!connection.source().isNoOp() && connection.destination().isNoOp()) {
				Entry<RouteKey, FlowNode> e = findNoOpReal(connection.key(), connection.destination());
				add(connection.source(), e.getValue(), e.getKey());
			}
		}
	}
	
	private Entry<RouteKey,FlowNode> findNoOpReal(RouteKey key, FlowNode current) {
		if (!current.isNoOp()) return createEntry(key, current);
		for (FlowNodeConnection connection : noOpConnections) {
			if (connection.source().equals(current)) {
				current = connection.destination();
				if (key == null) key = current.key();
			}
		}
		return findNoOpReal(key, current);
	}
	
	private void expand(FlowSegment segment) {
		
		if (!segment.equals(rootSegment) && segment.isNewContext()) {
			Path newContextFlowName;
			if (segment.label() != null) {
				newContextFlowName = flowName.add(segment.label());
			} else {
				newContextFlowName = flowName.add("context-" + newContextSegments.size());
			}
			newContextSegments.put(newContextFlowName, segment.clearNewContext());
			usesFlows.add(newContextFlowName);
			expand(new FlowReferenceNode(newContextFlowName.slashes(), newContextFlowName));
		} else {
		
			segments.add(segment);
			
			if (segment.isNode() && !segment.node().isNoOp()) {
				nodes.add(segment.node());
			}
			
			for (FlowConnection connection : segment.connections()) {
				connect(connection.source(), connection.destination(), null);
			}
			
			for (FlowSegment nested : segment.segments()) {
				if (!nested.equals(segment)) {
					expand(nested);
				}
			}
		
		}
		
	}
	
	private void add(FlowNode from, FlowNode to, RouteKey key) {
		boolean optional = from.node().isRouter();
		if (from.isNoOp() || to.isNoOp()) {
			noOpConnections.add(FlowNodeConnection.create(from, to, key, optional));			
		} else {
			FlowNodeConnection connection = FlowNodeConnection.create(from, to, key, optional);
			conns.add(connection);
			nodes.add(from);
			nodes.add(to);
			destinationsOf.put(from, to);
			sourcesOf.put(to, from);
			if (from.hasFlowReference()) register(from.flowReferenceNode());
			if (to.hasFlowReference()) register(to.flowReferenceNode());	
		}
	}
	
	private void register(FlowReference val) {
		usesFlows.add(val.flowName());
	}
	
	private Collection<FlowNode> tailsOf(FlowNode node, Collection<FlowNode> results) {
		Set<FlowNode> destinations = destinationsOf.get(node);
		if (destinations.isEmpty()) {
			results.add(node);
		} else {
			for (FlowNode destination : destinations) {
				tailsOf(destination, results);
			}
		}
		return results;
	}
	
	private void connect(FlowSegment sourceSegment, FlowSegment destinationSegment, RouteKey parentKey) {
		checkNotNull(sourceSegment, "source was null");
		checkNotNull(destinationSegment, "destination was null");
		
		segments.add(sourceSegment);
		segments.add(destinationSegment);
	    
		for (FlowConnection connection : sourceSegment.connections()) {
			connect(connection.source(), connection.destination(), null);
		}
		for (FlowConnection connection : destinationSegment.connections()) {
			connect(connection.source(), connection.destination(), null);
		}
		
		if (sourceSegment.destinations().size() > 1 && destinationSegment.sources().size() > 1) {
			
			// special case: multi output -> multi input, auto-create intermediate node

			FlowSegment intermediate = noop();
			
			for (FlowSegment from : sourceSegment.destinations()) {
				connect(from, intermediate, parentKey);
			}
			
			for (FlowSegment to : destinationSegment.sources()) {
				connect(intermediate, to, parentKey);
			}
			
		} else {

			for (FlowSegment to : destinationSegment.sources()) {
				
				for (FlowSegment from : sourceSegment.destinations()) {
					
					RouteKey key= firstNonNull( 
						parentKey,
						destinationSegment.key(), 
						to.key());
					
					if (from.isNode() && to.isNode()) {
						
						String middle;
						if (key != null) {
							middle = format("-- %s -->", key);
						} else {
							middle = "-->";
						}
						log.debug("node connection [{}] {} [{}]", from.label(), middle, to.label());
						
						add(from.node(), to.node(), key);
						
					} else {
						
						if (key != null && !key.equals(to.key())) {
							connect(from, to, key);
						} else {
							connect(from, to, null);
						}
						
					}
				}
			}
		
		}
	}
	
	@SafeVarargs
	private static <T> T firstNonNull(T... items) {
		for (T item : items) {
			if (item != null) {
				return item;
			}
		}
		return null;
	}
	
}