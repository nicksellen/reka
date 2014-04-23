package reka.core.runtime.handlers;

import reka.api.data.Data;
import reka.api.data.MutableData;
import reka.core.runtime.FlowContext;
import reka.core.runtime.Node;

public class RuntimeNode implements Node {
	
	private final int id;
	private final String name;
	
	private final ActionHandler next;
	private final HaltedHandler halted;
	private final ErrorHandler error;
	
	public RuntimeNode(int id, String name, ActionHandler next, HaltedHandler halted, ErrorHandler error) {
		this.id = id;
		this.name = name;
		this.next = next;
		this.halted = halted;
		this.error = error;
	}
	
	@Override
	public int id() {
		return id;
	}

	@Override
	public String name() {
		return name;
	}

	@Override
	public void call(MutableData data, FlowContext context) {
		context.execute(() -> {
			try {
				next.call(data, context);
			} catch (Throwable t) {
				error.error(data, context, t);
			}
		});
	}

	@Override
	public void halted(FlowContext context) {
		halted.halted(context);
	}

	@Override
	public void error(Data data, FlowContext context, Throwable t) {
		error.error(data, context, t);
	}
	
}
