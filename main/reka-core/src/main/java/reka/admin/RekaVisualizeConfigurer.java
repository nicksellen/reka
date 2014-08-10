package reka.admin;

import static reka.api.Path.dots;
import static reka.core.builder.FlowSegments.sync;

import java.util.function.Function;
import java.util.function.Supplier;

import reka.ApplicationManager;
import reka.api.Path;
import reka.api.Path.Response;
import reka.api.data.Data;
import reka.api.flow.FlowSegment;
import reka.config.configurer.annotations.Conf;
import reka.core.util.StringWithVars;

public class RekaVisualizeConfigurer implements Supplier<FlowSegment> {

	private final ApplicationManager manager;
	
	public RekaVisualizeConfigurer(ApplicationManager manager) {
		this.manager = manager;
	}
	
	private Function<Data,String> formatFn = (data) -> "dot";
	private Function<Data,String> appIdentityFn;
	private Function<Data,String> flowNameFn;
	private Path in;
	private Path out = Response.CONTENT;
	
	@Conf.At("in")
	public void in(String val) {
		in = dots(val);
	}
	
	@Conf.At("out")
	public void out(String val) {
		out = dots(val);
	}
	
	@Conf.At("app")
	public void identity(String val) {
		appIdentityFn = StringWithVars.compile(val);
	}
	
	@Conf.At("flow")
	public void flowName(String val) {
		flowNameFn = StringWithVars.compile(val);
	}
	
	@Conf.At("format")
	public void format(String val) {
		formatFn = StringWithVars.compile(val);
	}
	
	@Override
	public FlowSegment get() {
		if (in != null) {
			return sync("visualize", () -> new RekaVisualizeOperation(manager, in, formatFn, out));
		} else if (appIdentityFn != null) {
			return sync("visualize", () -> new VisualizeAppOperation(manager, appIdentityFn, flowNameFn, formatFn, out));
		} else {
			throw new RuntimeException("put the errors in the proper place nick!");
		}
	}
	
}