package reka.core.runtime.handlers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import reka.api.data.MutableData;
import reka.core.runtime.FlowContext;

import com.google.common.collect.ImmutableList;

public class ActionHandlers implements ActionHandler {
	
	public static ActionHandler combine(Collection<? extends ActionHandler> handlers, ErrorHandler error) {
		List<ActionHandler> dst = new ArrayList<>();
		addAll(handlers, dst);
		if (dst.isEmpty()) {
			return DoNothing.INSTANCE;
		} else if (dst.size() == 1) {
			return dst.get(0);
		} else {
			return new ActionHandlers(ImmutableList.copyOf(dst), error);
		}
	}

	// flattens all
	
	private static void addAll(Collection<? extends ActionHandler> src, List<ActionHandler> dst) {
		for (ActionHandler handler : src) {
			if (handler instanceof ActionHandlers) {
				ActionHandlers inner = (ActionHandlers) handler;
				addAll(inner.handlers, dst);
			} else if (!DoNothing.INSTANCE.equals(handler)){
				dst.add(handler);
			}
		}
	}
	
	private final Collection<ActionHandler> handlers;
	private final ErrorHandler error;
	
	private ActionHandlers(Collection<ActionHandler> handlers, ErrorHandler error) {
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
