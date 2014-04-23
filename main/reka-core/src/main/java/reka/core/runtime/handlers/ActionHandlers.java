package reka.core.runtime.handlers;

import java.util.Collection;

import reka.api.data.MutableData;
import reka.core.runtime.FlowContext;

public class ActionHandlers implements ActionHandler {
	
	private final Collection<? extends ActionHandler> handlers;
	
	public ActionHandlers(Collection<? extends ActionHandler> handlers) {
		this.handlers = handlers;
	}

	@Override
	public void call(MutableData data, FlowContext context) {
		for (ActionHandler handler : handlers) {
			try {
				handler.call(data, context);
			} catch (Throwable t) {
				t.printStackTrace();
				// the show must go on...
			}
		}
	}
	
}
