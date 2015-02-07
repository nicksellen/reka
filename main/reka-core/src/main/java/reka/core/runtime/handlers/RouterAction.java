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

		RouteCollector collector = DefaultRouteCollector.create(keys);
		
		context.operationExecutor().execute(() -> {
			
			operation.call(data, collector);
			
			context.coordinationExecutor().execute(() -> {

				boolean copy = collector.routed().size() > 1;
	
				for (NodeChild child : children) {
					if (collector.routed().contains(child.key())) {
						context.call(child.node(), error, copy ? data.mutableCopy() : data);
					} else {
						child.node().halted(context);
					}
				}
			
			});
			
		});

	}

}
