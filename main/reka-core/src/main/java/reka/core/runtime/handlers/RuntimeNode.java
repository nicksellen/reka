package reka.core.runtime.handlers;

import static java.lang.String.format;
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
		context.handleAction(next, error, data);
	}

	@Override
	public void halted(FlowContext context) {
		halted.halted(context);
	}

	@Override
	public void error(Data data, FlowContext context, Throwable t) {
		error.error(data, context, t);
	}
	
	@Override
	public String toString() {
		return format("%s(id=%s,name=%s,next=%s,halted=%s,error=%s)", 
				getClass().getSimpleName(), 
				id, 
				name, 
				next.getClass().getSimpleName(),
				halted.getClass().getSimpleName(),
				error.getClass().getSimpleName());
	}
	
}
