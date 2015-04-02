package reka.core.builder;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static reka.core.runtime.handlers.DSL.actionHandlers;
import static reka.core.runtime.handlers.DSL.backgroundOp;
import static reka.core.runtime.handlers.DSL.endAction;
import static reka.core.runtime.handlers.DSL.errorHandlers;
import static reka.core.runtime.handlers.DSL.haltedHandlers;
import static reka.core.runtime.handlers.DSL.op;
import static reka.core.runtime.handlers.DSL.routing;
import static reka.util.Util.runtime;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.api.flow.FlowNode;
import reka.api.flow.FlowOperation;
import reka.api.run.Execution;
import reka.api.run.ExecutionChoosingOperation;
import reka.api.run.RouteKey;
import reka.api.run.RouterOperation;
import reka.core.config.NoOp;
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

class NodeBuilder {
	
	private static final Logger log = LoggerFactory.getLogger(NodeBuilder.class);
	
	private final ExecutorService backgroundExecutor;
	
	private final int id;
	private final String name;
	private final List<Integer> listeners = new ArrayList<>();
	private final List<NodeChildBuilder> children = new ArrayList<>();
	private int initialCounter = 0;
	private int parentCount = 0;
	private boolean isTrigger; // only used if we are a waiting node, means it needs to trigger something further down the flow
	
	private final FlowNode node;
	
	public NodeBuilder(int id, String name, FlowNode node, ExecutorService backgroundExecutor) {
	    this.id = id;
		this.name = name;
		this.node = node;
		this.backgroundExecutor = backgroundExecutor;
	}
	
	public void addListener(int subscriber) {
		listeners.add(subscriber);
	}

	public void incrementInitialCounter() {
		initialCounter++;
	}
	
	public int initialCounter() {
		return initialCounter;
	}

	public NodeBuilder addChild(NodeBuilder child, RouteKey key) {
		children.add(NodeChildBuilder.create(child, key));
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
	
	public Collection<NodeChildBuilder> children() {
		return children;
	}
	
	private List<NodeChild> buildChildren(NodeFactory factory) {
		return children.stream().map(child -> child.build(factory)).collect(toList());   
	}
	
	private List<Node> buildListeners(NodeFactory factory) {
        return this.listeners.stream().map(factory::get).collect(toList());
	}
	
	private static final HaltedHandler CONTEXT_HALTED = FlowContext.DEFAULT_HALTED_HANDLER;
	private static final ErrorHandler CONTEXT_ERROR = FlowContext.DEFAULT_ERROR_HANDLER;
	
	private final <A extends B,B> List<B> listToList(List<A> a) {
		List<B> b = new ArrayList<>();
		for (A item : a) b.add(item);
		return b;
	}
	
	Node build(NodeFactory factory) {
	    
		String prefix = "";
		
		StringBuilder sb = new StringBuilder();
		
		List<NodeChild> children = buildChildren(factory);
		
		List<Node> listenerNodes = buildListeners(factory);
		List<FailureHandler> listeners = listToList(listenerNodes);
		
		boolean stateful = initialCounter > 1;
		boolean hasListeners = !listeners.isEmpty();
		
		final ActionHandler action;
		
		ErrorHandler error = CONTEXT_ERROR;
		HaltedHandler halted = null;
				
		if (node.isEnd()) {
			halted = CONTEXT_HALTED;
		}
		
		if (hasListeners) {
			sb.append("listeners(").append(listeners).append(") ");
			error = errorHandlers(asList(errorHandlers(listeners), error));
			halted = haltedHandlers(asList(haltedHandlers(listeners), halted));
			
			for (Node listenerNode: listenerNodes) {
				factory.dot().append(format("%s\"%s\" -> \"%s\" [style=\"dashed\"]\n", prefix, id, listenerNode.id()));
			}
			
		}
		
		FlowOperation operation = null;
		
		if (node.hasOperationSupplier()) {
			operation = node.operationSupplier().get();	
		} else if (!node.isNoOp() && !node.isEnd() && !node.hasFlowReference()) {
			throw new IllegalStateException(format("node [%s] must have supplier, be subscribable, or embedded flow reference", name()));
		}

		for (NodeChild child : children) {
			factory.dot().append(format("%s\"%s\" -> \"%s\" \n", prefix, id, child.node().id()));
		}
		
		if (operation instanceof RouterOperation) {
			sb.append("router ");
			action = routing((RouterOperation) operation, children, error);
		} else {
			
			List<ActionHandler> childActions = children.stream().map(NodeChild::node).collect(toList());
			ActionHandler next = actionHandlers(childActions, error);
			
			if (!childActions.isEmpty()) {
				sb.append("children(").append(childActions.size()).append(") ");
			}
			
			if (operation != null && !NoOp.INSTANCE.equals(operation)) {
				
				sb.append("operation(").append(operation.getClass().getSimpleName()).append(") ");
				
				Execution execution = Execution.context;
				
				if (operation instanceof ExecutionChoosingOperation) {
					execution = ((ExecutionChoosingOperation) operation).execution();
				}
				
				switch (execution) {
				case context:
					action = op(operation, next, error);
					break;
				case background:
					sb.append("background ");
					action = backgroundOp(operation, next, error, backgroundExecutor);
				default:
					throw runtime("unknown executor group %s", execution.toString());
				}
				
			} else if (node.hasFlowReference()) {
				sb.append("embedded ");
				action = new EmbeddedFlowAction(factory.getFlow(node.flowReferenceNode().flowName()), next, halted, error);
			} else {
				action = next;
			}
		}
		
		ActionHandler main = action;
		
		if (node.isEnd()) {
			sb.append("end ");
			main = endAction(main);
		}
		
		if (stateful) {
			sb.append("stateful ");
			ControlHandler stateHandler = new StatefulControl(id, initialCounter, main, halted, error);
			main = stateHandler;
			halted = stateHandler;
		}
		
		// TODO: is this ok?
		if (halted == null) {
			halted = DoNothing.INSTANCE;
		}

		//main = new TimeLoggerAction(id, main, error);

		checkNotNull(main, "main was null");
		checkNotNull(halted, "halted was null");
		checkNotNull(error, "error was null");
		
		RuntimeNode rtNode = new RuntimeNode(id, name, main, halted, error);
		log.debug("\n  built node {} -> \n    {}\n    {}", id, sb.toString().trim(), rtNode);
		
		factory.dot().append(format("%s\"%s\" [label=\"%s\"]\n", prefix, id, name));
		
		return rtNode;
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