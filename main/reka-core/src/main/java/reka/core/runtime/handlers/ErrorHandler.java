package reka.core.runtime.handlers;

import reka.api.data.Data;
import reka.core.runtime.FlowContext;

public interface ErrorHandler {
	void error(Data data, FlowContext context, Throwable t);
}