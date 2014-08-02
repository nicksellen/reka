package reka.command;

import static reka.core.builder.FlowSegments.sync;

import java.util.List;
import java.util.function.Supplier;

import reka.api.flow.FlowSegment;

public class RunCommandConfigurer implements Supplier<FlowSegment> {
	
	private final String exec;
	private final List<String> args;
	
	public RunCommandConfigurer(String exec, List<String> args) {
		this.exec = exec;
		this.args = args;
	}

	@Override
	public FlowSegment get() {
		return sync("run", () -> new RunCommandOperation(exec, args));
	}

}
