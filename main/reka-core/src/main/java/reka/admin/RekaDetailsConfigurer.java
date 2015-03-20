package reka.admin;

import static reka.api.Path.dots;

import java.util.function.Function;

import reka.api.Path;
import reka.api.data.Data;
import reka.config.configurer.annotations.Conf;
import reka.core.app.manager.ApplicationManager;
import reka.core.setup.OperationConfigurer;
import reka.core.setup.OperationSetup;
import reka.core.util.StringWithVars;

public class RekaDetailsConfigurer implements OperationConfigurer {
	
	private final ApplicationManager manager;
	
	private Path out = dots("app");
	private Function<Data,String> idFn;
	
	@Conf.Val
	@Conf.At("out")
	@Conf.At("into")
	public void out(String val) {
		out = dots(val);
	}
	
	@Conf.At("id")
	public void app(String val) {
		idFn = StringWithVars.compile(val);
	}
	
	public RekaDetailsConfigurer(ApplicationManager manager) {
		this.manager = manager;
	}
	
	@Override
	public void setup(OperationSetup ops) {
		ops.add("get", ctx -> new RekaDetailsOperation(manager, idFn, out));
	}

}
