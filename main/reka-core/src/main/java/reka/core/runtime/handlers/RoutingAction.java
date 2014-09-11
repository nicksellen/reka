package reka.core.runtime.handlers;

import java.util.Collection;
import java.util.Set;

import reka.api.data.MutableData;
import reka.api.run.RoutingOperation;
import reka.core.runtime.DefaultRouter;
import reka.core.runtime.FlowContext;
import reka.core.runtime.NodeChild;

import com.google.common.collect.ImmutableSet;

public class RoutingAction implements ActionHandler {

	private final RoutingOperation operation;
	
	private final Collection<NodeChild> children;
	private final Set<String> childrenNames;
	
	public RoutingAction(RoutingOperation operation, Collection<NodeChild> children) {
		this.operation = operation;
		this.children = children;
		
		ImmutableSet.Builder<String> keys = ImmutableSet.builder();
		for (NodeChild child : children) {
		    if (child.name() != null) {
		        keys.add(child.name());
		    }
		}
		this.childrenNames = keys.build();
	}
	
	@Override
	public void call(MutableData data, FlowContext context) {
		
		DefaultRouter mr = DefaultRouter.create(childrenNames);
		
		operation.call(data, mr);
		
		boolean copy = mr.routed().size() > 1;
		
		for (NodeChild child : children) {
			if (mr.routed().contains(child.name())) {
				child.node().call(copy ? data.mutableCopy() : data, context);
			} else {
				child.node().halted(context);
			}
		}
	}

}
