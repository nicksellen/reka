package reka.runtime.handlers;

import reka.data.Data;
import reka.runtime.FlowContext;

public interface ErrorHandler {
	void error(Data data, FlowContext context, Throwable t);
}