package reka.runtime.handlers;

import reka.data.MutableData;
import reka.runtime.FlowContext;

public interface ActionHandler {
	void call(MutableData data, FlowContext context);
}