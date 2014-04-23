package reka.core.builder;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static reka.core.builder.FlowSegments.noop;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.api.Path;
import reka.api.flow.FlowConnection;
import reka.api.flow.FlowDependency;
import reka.api.flow.FlowNode;
import reka.api.flow.FlowSegment;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.SetMultimap;

public class FlowConnector {
	
	private final Logger log = LoggerFactory.getLogger(getClass());

	public static FlowConnector connectSegments(FlowSegment... segments) {
		return connectSegments(Arrays.asList(segments));
	}
	
	public static FlowConnector connectSegments(Collection<FlowSegment> segments) {
		return new FlowConnector(segments);
	}

	private final static Logger logger = LoggerFactory.getLogger("flow-connections");
	
	private final Set<FlowSegment> segments = new HashSet<>();
	
	private final Set<FlowNodeConnection> conns = new HashSet<>();
	private final Set<FlowNode> nodes = new HashSet<>();
	private final SetMultimap<FlowNode,FlowNode> destinationsOf = HashMultimap.create();
	private final SetMultimap<FlowNode,FlowNode> sourcesOf = HashMultimap.create();
	
	private final Set<Path> usesFlows = new HashSet<>();
	
	public FlowConnector() {
		this(Collections.emptyList());
	}
	
	private FlowConnector(Iterable<FlowSegment> segments) {
		for (FlowSegment segment : segments) {
			expand(segment);
		}
		//checkState(!conns.isEmpty(), "didn't find ANY connections :(");
	}
	
	private void expandNodes(FlowSegment segment) {
		if (segment.isNode()) {
			nodes.add(segment.node());
		}
		for (FlowSegment inner : segment.segments()) {
			if (!inner.equals(segment)) {
				expandNodes(inner);
			}
		}
	}
	
	public void expand(FlowSegment segment) {
		
		segments.add(segment);
		
		expandNodes(segment);
		
		for (FlowConnection connection : segment.connections()) {
			connect(connection.source(), connection.destination(), connection.name());
		}
		
		for (FlowSegment nested : segment.segments()) {
			if (!nested.equals(segment)) {
				expand(nested);
			}
		}
		
	}
	
	public void add(FlowSegment from, FlowSegment to) {
		connect(from, to, null);
	}
	
	private void add(FlowNode from, FlowNode to, String name, boolean optional) {
		FlowNodeConnection connection = FlowNodeConnection.create(from, to, name, optional);
		conns.add(connection);
		nodes.add(from);
		nodes.add(to);
		destinationsOf.put(from, to);
		sourcesOf.put(to, from);
		
		if (from.hasEmbeddedFlow()) register(from.embeddedFlowNode());
		if (to.hasEmbeddedFlow()) register(to.embeddedFlowNode());
	}
	
	private void register(FlowDependency val) {
		usesFlows.add(val.flowName());
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
	
	private void connect(FlowSegment sourceSegment, FlowSegment destinationSegment, String name) {
		checkNotNull(sourceSegment, "source was null");
		checkNotNull(destinationSegment, "destination was null");
		
		// TODO: probably being ineffecient here...
		segments.add(sourceSegment);
		segments.add(destinationSegment);
	    
		for (FlowConnection connection : sourceSegment.connections()) {
			connect(connection.source(), connection.destination(), connection.name());
		}
		for (FlowConnection connection : destinationSegment.connections()) {
			connect(connection.source(), connection.destination(), connection.name());
		}
		
		if (sourceSegment.destinations().size() > 1 && destinationSegment.sources().size() > 1) {
			
			// special case: multi output -> multi input, auto-create intermediate node

			FlowSegment intermediate = noop(format("+ [%s]", UUID.randomUUID().toString()));
			
			for (FlowSegment from : sourceSegment.destinations()) {
				connect(from, intermediate, name);
			}
			
			for (FlowSegment to : destinationSegment.sources()) {
				connect(intermediate, to, name);
			}
			
		} else {

			for (FlowSegment to : destinationSegment.sources()) {
				
				for (FlowSegment from : sourceSegment.destinations()) {
					
					String connectionName = firstNonNull( 
						name,
						sourceSegment.outputName(), 
						from.outputName(),
						destinationSegment.inputName(), 
						to.inputName());
					
					if (from.isNode() && to.isNode()) {
						
						String middle;
						if (connectionName != null) {
							middle = format("-- %s -->", connectionName);
						} else {
							middle = "-->";
						}
						logger.debug("node connection [{}] {} [{}]", from.label(), middle, to.label());
						
						// TODO: work out how to know if it's optional here? (I don't think I can)
						add(from.node(), to.node(), connectionName, false);
						
					} else {
						
						connect(from, to, 
								connectionName != null && !connectionName.equals(to.inputName()) ? connectionName : null);
						
					}
				}
			}
		
		}
	}
	
	private static String firstNonNull(String... items) {
		for (String item : items) {
			if (item != null) {
				return item;
			}
		}
		return null;
	}
	
}