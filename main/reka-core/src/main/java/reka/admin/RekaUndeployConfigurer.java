package reka.admin;

import static reka.core.builder.FlowSegments.sync;

import java.util.function.Function;
import java.util.function.Supplier;

import reka.ApplicationManager;
import reka.api.data.Data;
import reka.api.flow.FlowSegment;
import reka.configurer.annotations.Conf;
import reka.core.util.StringWithVars;

public class RekaUndeployConfigurer implements Supplier<FlowSegment> {

	private final ApplicationManager manager;
	private Function<Data,String> identityFn;
	
	RekaUndeployConfigurer(ApplicationManager manager) {
		this.manager = manager;
	}
	
	@Conf.Val
	@Conf.At("identity")
	public void in(String val) {
		identityFn = StringWithVars.compile(val);
	}
	
	@Override
	public FlowSegment get() {
		return sync("deploy", () -> new RekaUndeployOperation(manager, identityFn));
	}
	
}