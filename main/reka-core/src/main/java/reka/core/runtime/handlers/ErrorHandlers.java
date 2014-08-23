package reka.core.runtime.handlers;

import java.util.Collection;

import reka.api.data.Data;
import reka.core.runtime.FlowContext;

public class ErrorHandlers implements ErrorHandler {
	
	private final Collection<? extends ErrorHandler> handlers;
	
	public ErrorHandlers(Collection<? extends ErrorHandler> handlers) {
		this.handlers = handlers;
	}

	@Override
	public void error(Data data, FlowContext context, Throwable t) {
		for (ErrorHandler handler : handlers) {
			try {
				handler.error(data, context, t);
			} catch (Throwable t2) {
				// the show must go on...
			}
		}		
	}

}
