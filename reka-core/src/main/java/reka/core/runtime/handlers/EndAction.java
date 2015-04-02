package reka.core.runtime.handlers;

import reka.api.data.MutableData;
import reka.core.runtime.FlowContext;

public class EndAction implements ActionHandler {

	@Override
	public void call(MutableData data, FlowContext context) {
		context.end(data);
	}

}
