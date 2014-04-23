package reka.core.runtime.handlers;

import reka.api.data.Data;
import reka.api.data.MutableData;
import reka.core.runtime.FailureHandler;
import reka.core.runtime.FlowContext;

public final class DoNothing implements ControlHandler, ActionHandler, HaltedHandler, ErrorHandler, FailureHandler {

	public static final DoNothing INSTANCE = new DoNothing();

	@Override
	public void error(Data data, FlowContext context, Throwable t) { /* nothing! */ }

	@Override
	public void halted(FlowContext context) { /* nothing! */ }

	@Override
	public void call(MutableData data, FlowContext context) { /* nothing! */ }
	
}