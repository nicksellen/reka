package reka.core.runtime.handlers;

import static java.lang.String.format;
import static reka.util.Util.unsupported;

import java.util.Collection;
import java.util.concurrent.ExecutorService;

import reka.api.flow.FlowOperation;
import reka.api.run.AsyncOperation;
import reka.api.run.Operation;
import reka.api.run.RouterOperation;
import reka.api.run.Subscriber;
import reka.core.config.NoOp;
import reka.core.runtime.NodeChild;
import reka.core.runtime.handlers.stateful.StatefulControl;

public class DSL {
	
	public static StatefulControl stateful(int id, int initialCount, ActionHandler next, HaltedHandler halt, ErrorHandler error) {
		return new StatefulControl(id, initialCount, next, halt, error);
	}
	
	public static ActionHandler op(FlowOperation operation, ActionHandler next, ErrorHandler error) {
		if (operation instanceof Operation) {
			return new OperationAction((Operation) operation, next);
		} else if (operation instanceof AsyncOperation) {
			return new AsyncOperationAction((AsyncOperation) operation, next, error);
		} else if (operation instanceof NoOp) {
			return next; // NoOp does nothing so skip to the next immediately...
		} else {
			throw unsupported("sorry, haven't adding in handling for %s operations yet", operation.getClass().getSimpleName());
		}
	}

	public static ActionHandler backgroundOp(FlowOperation operation, ActionHandler next, ErrorHandler error, ExecutorService backgroundExecutor) {
		
		AsyncOperation asyncOperation = null;
		
		if (operation instanceof Operation) {
			asyncOperation = ((Operation) operation).asAsync();
		} else if (operation instanceof AsyncOperation) {
			asyncOperation = (AsyncOperation) operation;
		}
		
		if (asyncOperation != null) {
			return new BackgroundAsyncOperationAction(asyncOperation, next, error, backgroundExecutor);
		} else {
			return next;
		}
	}
	
	public static RouterAction routing(RouterOperation operation, Collection<NodeChild> children, ErrorHandler error) {
		return new RouterAction(operation, children, error);
	}
	
	public static ActionHandler endAction(ActionHandler next) {
		if (next == DoNothing.INSTANCE) {
			return new EndAction();
		} else {
			throw new IllegalStateException(
				format("was excepting this to be a DoNothing action, not %s", next.getClass()));
		}
	}
	
	public static SubscribersAction subscriber(Subscriber subscriber) {
		return subscribers(subscriber);
	}
	
	public static SubscribersAction subscribers(Subscriber... subscribers) {
		return new SubscribersAction(subscribers);
	}
	
	public static ActionHandler actionHandlers(Collection<? extends ActionHandler> handlers, ErrorHandler error) {
		return ActionHandlers.combine(handlers, error);
	}

	public static HaltedHandler haltedHandlers(Collection<? extends HaltedHandler> handlers) {
		return HaltedHandlers.combine(handlers);
	}
	
	public static ErrorHandler errorHandlers(Collection<? extends ErrorHandler> handlers) {
		return ErrorHandlers.combine(handlers);
	}
	
}
