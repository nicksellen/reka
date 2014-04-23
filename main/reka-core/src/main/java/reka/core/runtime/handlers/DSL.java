package reka.core.runtime.handlers;

import static java.util.stream.Collectors.toList;
import static reka.util.Util.unsupported;

import java.util.Collection;

import reka.api.flow.FlowOperation;
import reka.api.run.AsyncOperation;
import reka.api.run.RoutingOperation;
import reka.api.run.Subscriber;
import reka.api.run.SyncOperation;
import reka.core.config.NoOp;
import reka.core.runtime.NodeChild;
import reka.core.runtime.handlers.stateful.StatefulControl;

public class DSL {
	
	public static StatefulControl stateful(int id, int initialCount, ActionHandler next, HaltedHandler halt, ErrorHandler error) {
		return new StatefulControl(id, initialCount, next, halt, error);
	}
	
	public static ActionHandler op(FlowOperation operation, ActionHandler next, ErrorHandler error) {
		if (operation instanceof SyncOperation) {
			return syncOperation((SyncOperation) operation, next);
		} else if (operation instanceof AsyncOperation) {
			return asyncOperation((AsyncOperation) operation, next, error);
		} else if (operation instanceof NoOp) {
			return next; // NoOp does nothing so skip to the next immediately...
		} else {
			throw unsupported("sorry, haven't adding in handling for %s operations yet", operation.getClass().getSimpleName());
		}
	}
	
	public static RoutingAction routing(RoutingOperation operation, Collection<NodeChild> children) {
		return new RoutingAction(operation, children);
	}
	
	public static ActionHandler subscribableAction(ActionHandler next) {
		return new CallSubscriberAction(next);
	}

	public static SyncAction syncOperation(SyncOperation operation, ActionHandler next) {
		return new SyncAction(operation, next);
	}
	
	public static SyncAction syncOperation(SyncOperation operation) {
		return syncOperation(operation, DoNothing.INSTANCE);
	}
	
	public static AsyncAction asyncOperation(AsyncOperation operation, ActionHandler next, ErrorHandler error) {
		return new AsyncAction(operation, next, error);
	}
	
	public static SubscribersAction subscriber(Subscriber subscriber) {
		return subscribers(subscriber);
	}
	
	public static SubscribersAction subscribers(Subscriber... subscribers) {
		return new SubscribersAction(subscribers);
	}
	
	public static ActionHandler actionHandlers(Collection<? extends ActionHandler> handlers) {
		handlers = handlers.stream().filter((f) -> !f.equals(DoNothing.INSTANCE)).collect(toList());
		switch (handlers.size()) {
		case 0: return DoNothing.INSTANCE;
		case 1: return handlers.iterator().next();
		default: return new ActionHandlers(handlers);
		}
		
	}

	public static HaltedHandler haltedHandlers(Collection<? extends HaltedHandler> handlers) {
		handlers = handlers.stream().filter((f) -> !f.equals(DoNothing.INSTANCE)).collect(toList());
		switch (handlers.size()) {
		case 0: return DoNothing.INSTANCE;
		case 1: return handlers.iterator().next();
		default: return new HaltedHandlers(handlers);
		}
	}
	
	public static ErrorHandler errorHandlers(Collection<? extends ErrorHandler> handlers) {
		handlers = handlers.stream().filter((f) -> !f.equals(DoNothing.INSTANCE)).collect(toList());
		switch (handlers.size()) {
		case 0: return DoNothing.INSTANCE;
		case 1: return handlers.iterator().next();
		default: return new ErrorHandlers(handlers);
		}
	}
	
}
