package reka.admin;

import static reka.api.Path.dots;

import java.util.function.Function;

import reka.ApplicationManager;
import reka.api.Path;
import reka.api.data.Data;
import reka.config.configurer.annotations.Conf;
import reka.core.setup.OperationConfigurer;
import reka.core.setup.OperationSetup;
import reka.core.util.StringWithVars;

public class RekaDetailsConfigurer implements OperationConfigurer {
	
	private final ApplicationManager manager;
	
	private Path out = dots("app");
	private Function<Data,String> idFn;
	
	@Conf.Val
	@Conf.At("out")
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
		ops.add("get", store -> new RekaDetailsOperation(manager, idFn, out));
	}

}
