package reka.core.runtime.handlers;

import java.util.Collection;

import reka.api.data.MutableData;
import reka.core.runtime.FlowContext;

public class ActionHandlers implements ActionHandler {
	
	private final Collection<? extends ActionHandler> handlers;
	private final ErrorHandler error;
	
	public ActionHandlers(Collection<? extends ActionHandler> handlers, ErrorHandler error) {
		// TODO: flatten if any of the handlers passed in is itself an ActionHandlers
		this.handlers = handlers;
		this.error = error;
	}

	@Override
	public void call(MutableData data, FlowContext context) {
		for (ActionHandler handler : handlers) {
			try {
				handler.call(data.mutableCopy(), context);
			} catch (Throwable t) {
				error.error(data, context, t);
			}
		}
	}
	
}
