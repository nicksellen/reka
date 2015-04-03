package reka.runtime.handlers;

import reka.data.MutableData;
import reka.runtime.FlowContext;

public class EndAction implements ActionHandler {

	@Override
	public void call(MutableData data, FlowContext context) {
		context.end(data);
	}

}
