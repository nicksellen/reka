package reka.core.runtime.handlers;

import java.util.Collection;
import java.util.Set;

import reka.api.data.MutableData;
import reka.api.run.RouteCollector;
import reka.api.run.RouteKey;
import reka.api.run.RouterOperation;
import reka.core.runtime.DefaultRouteCollector;
import reka.core.runtime.FlowContext;
import reka.core.runtime.NodeChild;

import com.google.common.collect.ImmutableSet;

public class RouterAction implements ActionHandler {

	private final RouterOperation operation;
	
	private final Collection<NodeChild> children;
	private final Set<RouteKey> keys;
	
	public RouterAction(RouterOperation operation, Collection<NodeChild> children) {
		this.operation = operation;
		this.children = children;
		
		ImmutableSet.Builder<RouteKey> keys = ImmutableSet.builder();
		for (NodeChild child : children) {
		    if (child.key() != null) {
		        keys.add(child.key());
		    }
		}
		
		this.keys = keys.build();
	}
	
	@Override
	public void call(MutableData data, FlowContext context) {
		
		RouteCollector collector = DefaultRouteCollector.get(keys);
		
		operation.call(data, collector);
		
		boolean copy = collector.routed().size() > 1;
		
		for (NodeChild child : children) {
			if (collector.routed().contains(child.key())) {
				child.node().call(copy ? data.mutableCopy() : data, context);
			} else {
				child.node().halted(context);
			}
		}
		
	}

}

