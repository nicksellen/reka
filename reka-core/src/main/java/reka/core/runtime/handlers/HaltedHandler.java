package reka.core.runtime.handlers;

import reka.core.runtime.FlowContext;

public interface HaltedHandler {
	void halted(FlowContext context);
}