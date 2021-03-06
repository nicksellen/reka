package reka.modules.admin;

import static reka.util.Path.dots;

import java.util.function.Function;

import reka.app.manager.ApplicationManager;
import reka.config.configurer.annotations.Conf;
import reka.data.Data;
import reka.module.setup.OperationConfigurer;
import reka.module.setup.OperationSetup;
import reka.util.Path;
import reka.util.Path.Response;
import reka.util.StringWithVars;

public class RekaVisualizeConfigurer implements OperationConfigurer {

	private final ApplicationManager manager;
	
	public RekaVisualizeConfigurer(ApplicationManager manager) {
		this.manager = manager;
	}
	
	private Function<Data,String> formatFn = (data) -> "dot";
	private Function<Data,Path> appPathFn;
	private Function<Data,String> flowNameFn;
	private String stylesheet;
	private Path out = Response.CONTENT;
	
	@Conf.At("out")
	@Conf.At("into")
	public void out(String val) {
		out = dots(val);
	}
	
	@Conf.At("stylesheet")
	public void stylesheet(String val) {
		stylesheet = val;
	}
	
	@Conf.At("app")
	public void identity(String val) {
		appPathFn = StringWithVars.compile(val).andThen(Path::slashes);
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
		if (appPathFn != null) {
			ops.add("visualize", () -> new VisualizeAppOperation(manager, appPathFn, flowNameFn, formatFn, out, stylesheet));
		} else {
			throw new RuntimeException("put the errors in the proper place nick!");
		}
	}
	
}