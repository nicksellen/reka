package reka.runtime.handlers;

import reka.data.Data;
import reka.data.MutableData;
import reka.runtime.FailureHandler;
import reka.runtime.FlowContext;

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