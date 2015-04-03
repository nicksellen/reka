package reka.runtime.handlers;

import java.util.Collection;
import java.util.Set;

import reka.data.MutableData;
import reka.flow.ops.RouteCollector;
import reka.flow.ops.RouteKey;
import reka.flow.ops.RouterOperation;
import reka.runtime.DefaultRouteCollector;
import reka.runtime.FlowContext;
import reka.runtime.NodeChild;

import com.google.common.collect.ImmutableSet;

public class RouterAction implements ActionHandler {

	private final RouterOperation operation;
	private final ErrorHandler error;

	private final Collection<NodeChild> children;
	private final Set<RouteKey> keys;

	public RouterAction(RouterOperation operation, Collection<NodeChild> children, ErrorHandler error) {
		this.operation = operation;
		this.children = children;
		this.error = error;

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
		
		context.operationExecutor().execute(() -> {

			RouteCollector collector = DefaultRouteCollector.create(keys);
			
			operation.call(data, collector);

			boolean copy = collector.routed().size() > 1;

			for (NodeChild child : children) {
				if (collector.routed().contains(child.key())) {
					context.handleAction(child.node(), error, copy ? data.mutableCopy() : data);
				} else {
					context.handleHalted(child.node());
				}
			}
			
		});

	}

}
