package reka.core.runtime.handlers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.google.common.collect.ImmutableList;

import reka.api.data.Data;
import reka.core.runtime.FlowContext;

public class ErrorHandlers implements ErrorHandler {

	public static ErrorHandler combine(Collection<? extends ErrorHandler> handlers) {
		List<ErrorHandler> dst = new ArrayList<>();
		addAll(handlers, dst);
		if (dst.isEmpty()) {
			return DoNothing.INSTANCE;
		} else if (dst.size() == 1) {
			return dst.get(0);
		} else {
			return new ErrorHandlers(ImmutableList.copyOf(dst));
		}
	}

	// flattens all
	
	private static void addAll(Collection<? extends ErrorHandler> src, List<ErrorHandler> dst) {
		for (ErrorHandler handler : src) {
			if (handler instanceof ErrorHandlers) {
				ErrorHandlers inner = (ErrorHandlers) handler;
				addAll(inner.handlers, dst);
			} else if (!DoNothing.INSTANCE.equals(handler)){
				dst.add(handler);
			}
		}
	}
	
	private final Collection<ErrorHandler> handlers;
	
	private ErrorHandlers(Collection<ErrorHandler> handlers) {
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
