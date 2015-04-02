package reka.core.runtime.handlers;

import reka.api.data.MutableData;
import reka.core.runtime.FlowContext;

public interface ActionHandler {
	void call(MutableData data, FlowContext context);
}