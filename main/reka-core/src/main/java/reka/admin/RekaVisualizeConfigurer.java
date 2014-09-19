package reka.admin;

import static reka.api.Path.dots;

import java.util.function.Function;

import reka.ApplicationManager;
import reka.api.Path;
import reka.api.Path.Response;
import reka.api.data.Data;
import reka.config.configurer.annotations.Conf;
import reka.core.setup.OperationSetup;
import reka.core.util.StringWithVars;
import reka.nashorn.OperationConfigurer;

public class RekaVisualizeConfigurer implements OperationConfigurer {

	private final ApplicationManager manager;
	
	public RekaVisualizeConfigurer(ApplicationManager manager) {
		this.manager = manager;
	}
	
	private Function<Data,String> formatFn = (data) -> "dot";
	private Function<Data,String> appIdentityFn;
	private Function<Data,String> flowNameFn;
	private Path out = Response.CONTENT;
	
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
	public void setup(OperationSetup ops) {
		if (appIdentityFn != null) {
			ops.add("visualize", store -> new VisualizeAppOperation(manager, appIdentityFn, flowNameFn, formatFn, out));
		} else {
			throw new RuntimeException("put the errors in the proper place nick!");
		}
	}
	
}