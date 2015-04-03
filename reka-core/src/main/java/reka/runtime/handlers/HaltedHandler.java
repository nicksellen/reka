package reka.runtime.handlers;

import reka.runtime.FlowContext;

public interface HaltedHandler {
	void halted(FlowContext context);
}