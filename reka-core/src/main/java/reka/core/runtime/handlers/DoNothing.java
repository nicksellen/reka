package reka.core.runtime.handlers;

import reka.api.data.Data;
import reka.api.data.MutableData;
import reka.core.runtime.FailureHandler;
import reka.core.runtime.FlowContext;

public final class DoNothing implements ControlHandler, ActionHandler, HaltedHandler, ErrorHandler, FailureHandler {

	public static final DoNothing INSTANCE = new DoNothing();

	@Override
	public void error(Data data, FlowContext context, Throwable t) {
		System.err.printf("errored! but we did nothing! - %s\n", t.getMessage());
	}

	@Override
	public void halted(FlowContext context) { 
		System.err.printf("application halted but we did nothing!\n");
	}

	@Override
	public void call(MutableData data, FlowContext context) { /* nothing! */ }
	
}