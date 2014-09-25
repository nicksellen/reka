package reka.core.runtime.handlers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import reka.core.runtime.FlowContext;

import com.google.common.collect.ImmutableList;

public class HaltedHandlers implements HaltedHandler {
	
	public static HaltedHandler combine(Collection<? extends HaltedHandler> handlers) {
		List<HaltedHandler> dst = new ArrayList<>();
		addAll(handlers, dst);
		if (dst.isEmpty()) {
			return DoNothing.INSTANCE;
		} else if (dst.size() == 1) {
			return dst.get(0);
		} else {
			return new HaltedHandlers(ImmutableList.copyOf(dst));
		}
	}

	// flattens all
	
	private static void addAll(Collection<? extends HaltedHandler> src, List<HaltedHandler> dst) {
		for (HaltedHandler handler : src) {
			if (handler instanceof HaltedHandlers) {
				HaltedHandlers inner = (HaltedHandlers) handler;
				addAll(inner.handlers, dst);
			} else if (handler != null && !DoNothing.INSTANCE.equals(handler)){
				dst.add(handler);
			}
		}
	}
	
	private final Collection<HaltedHandler> handlers;
	
	private HaltedHandlers(Collection<HaltedHandler> handlers) {
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
