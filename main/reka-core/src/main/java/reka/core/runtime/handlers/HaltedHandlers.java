package reka.core.runtime.handlers;

import java.util.Collection;

import reka.core.runtime.FlowContext;

public class HaltedHandlers implements HaltedHandler {
	
	private final Collection<? extends HaltedHandler> handlers;
	
	public HaltedHandlers(Collection<? extends HaltedHandler> handlers) {
		this.handlers = handlers;
	}

	@Override
	public void halted(FlowContext context) {
		for (HaltedHandler handler : handlers) {
			try {
				handler.halted(context);
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}		
	}

}
