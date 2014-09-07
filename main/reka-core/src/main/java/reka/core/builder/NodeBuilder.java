package reka.core.builder;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static reka.core.runtime.handlers.DSL.actionHandlers;
import static reka.core.runtime.handlers.DSL.errorHandlers;
import static reka.core.runtime.handlers.DSL.haltedHandlers;
import static reka.core.runtime.handlers.DSL.op;
import static reka.core.runtime.handlers.DSL.routing;
import static reka.core.runtime.handlers.DSL.subscribableAction;
import static reka.util.Util.unwrap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import reka.api.data.Data;
import reka.api.flow.FlowNode;
import reka.api.flow.FlowOperation;
import reka.api.run.EverythingSubscriber;
import reka.api.run.RoutingOperation;
import reka.api.run.Subscriber;
import reka.api.run.SyncOperation;
import reka.core.runtime.FailureHandler;
import reka.core.runtime.FlowContext;
import reka.core.runtime.Node;
import reka.core.runtime.NodeChild;
import reka.core.runtime.handlers.ActionHandler;
import reka.core.runtime.handlers.ControlHandler;
import reka.core.runtime.handlers.DoNothing;
import reka.core.runtime.handlers.ErrorHandler;
import reka.core.runtime.handlers.HaltedHandler;
import reka.core.runtime.handlers.RuntimeNode;
import reka.core.runtime.handlers.stateful.StatefulControl;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ListeningExecutorService;

class NodeBuilder {
	
	private final ListeningExecutorService executor;
	
	private final int id;
	private final String name;
	private final List<Integer> listeners = new ArrayList<>();
	private final List<NodeChildBuilder> children = new ArrayList<>();
	private int initialCounter = 0;
	private int parentCount = 0;
	private boolean isTrigger; // only used if we are a waiting node, means it needs to trigger something further down the flow
	
	private final FlowNode node;
	
	public NodeBuilder(int id, String name, FlowNode node, ListeningExecutorService executor) {
	    this.id = id;
		this.name = name;
		this.node = node;
		this.executor = executor;
	}
	
	public void addListener(int subscriber) {
		listeners.add(subscriber);
	}

	public void incrementInitialCounter() {
		initialCounter++;
	}
	
	public int initialRemainingCount() {
		return initialCounter;
	}

	public NodeBuilder addChild(NodeBuilder child, String alias) {
		children.add(NodeChildBuilder.create(child, alias));
		return this;
	}
	
	public boolean isTrigger() {
		return isTrigger;
	}
	
	public void isTrigger(boolean value) {
		isTrigger = value;
	}
	
	public FlowNode node() {
		return node;
	}

	public boolean isRouterNode() {
		if (!node.hasOperationSupplier()) return false;
		return node.operationSupplier().isRouter();
	}
	
	public Collection<NodeChildBuilder> children() {
		return children;
	}
	
	private List<NodeChild> buildChildren(NodeFactory factory) {
        ImmutableList.Builder<NodeChild> result = new ImmutableList.Builder<>();
        for (NodeChildBuilder child : children) {
            result.add(child.build(factory));
        }
        return result.build();   
	}
	
	private List<FailureHandler> buildListeners(NodeFactory factory) {
        ImmutableList.Builder<FailureHandler> result = new ImmutableList.Builder<>();
        for (Integer listener : listeners) {
            result.add(factory.get(listener));
        }
        return result.build();
	}
	
	private static class NotifySubscriberOnHalted implements HaltedHandler {

		@Override
		public void halted(FlowContext context) {
			Subscriber subscriber = context.subscriber();
			if (subscriber instanceof EverythingSubscriber) {
				((EverythingSubscriber) subscriber).halted();
			}
		}
		
	}
	
	private static class NotifySubscriberOnError implements ErrorHandler {

		@Override
		public void error(Data data, FlowContext context, Throwable t) {
			t = unwrap(t);
			Subscriber subscriber = context.subscriber();
			if (subscriber instanceof EverythingSubscriber) {
				((EverythingSubscriber) subscriber).error(data, t);
			}
		}
		
	}
	
	private static final HaltedHandler NOTIFY_SUBSCRIBER_ON_HALTED = new NotifySubscriberOnHalted();
	private static final ErrorHandler NOTIFY_SUBSCRIBER_ON_ERROR = new NotifySubscriberOnError();
	
	Node build(NodeFactory factory) {
	    
		List<NodeChild> children = buildChildren(factory);
		List<FailureHandler> listeners = buildListeners(factory);
		
		boolean stateful = initialCounter > 1;
		boolean hasListeners = !listeners.isEmpty();
		
		final ActionHandler action;
		
		ErrorHandler error = NOTIFY_SUBSCRIBER_ON_ERROR;
		HaltedHandler halted = DoNothing.INSTANCE;
		
		if (hasListeners) {
			error = errorHandlers(asList(errorHandlers(listeners), error));
			halted = haltedHandlers(asList(haltedHandlers(listeners), halted));
		}

		if (node.isSubscribeable()) {
			halted = haltedHandlers(asList(halted, NOTIFY_SUBSCRIBER_ON_HALTED));
		}
		
		FlowOperation operation = null;
		
		if (node.hasOperationSupplier()) {
			operation = node.operationSupplier().get();
		} else if (node.isSubscribeable()) {
		} else if (!node.hasEmbeddedFlow()) {
			throw new IllegalStateException(format("node [%s] must have supplier or embedded flow reference", name()));
		}
		
		if (operation instanceof RoutingOperation) {
			action = routing((RoutingOperation) operation, children);
		} else {
			List<ActionHandler> childActions = children.stream().map(NodeChild::node).collect(toList());
			ActionHandler next = actionHandlers(childActions);
			
			if (operation != null) {
				if (operation instanceof SyncOperation && node.shouldUseAnotherThread()) {
					operation = ((SyncOperation) operation).toAsync(executor);
				}
				action = op(operation, next, error);
				
			} else if (node.hasEmbeddedFlow()) {
				action = new EmbeddedFlowAction(factory.getFlow(node.embeddedFlowNode().flowName()), next, halted, error);
			} else {
				action = next;
			}
		}
		
		ActionHandler main = action;
		
		if (node.isSubscribeable()) {
			main = subscribableAction(main);
		}

		//main = new TimeLoggerAction(id, main);
		
		if (stateful) {
			ControlHandler stateHandler = new StatefulControl(id, initialCounter, main, halted, error);
			main = stateHandler;
			halted = stateHandler;
		}
		
		return new RuntimeNode(id, name, main, halted, error);
	}

	@Override
	public String toString() {
		return format("%s", name);
	}

	public void incrementParentCount() {
		parentCount += 1;
	}
	
	public int parentCount() {
		return parentCount;
	}
	
	public String name() {
		return name;
	}
	
	public int id() {
	    return id;
	}
	
}