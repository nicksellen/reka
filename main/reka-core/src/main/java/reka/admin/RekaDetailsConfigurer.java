package reka.admin;

import static reka.api.Path.dots;
import static reka.core.builder.FlowSegments.sync;

import java.util.function.Function;
import java.util.function.Supplier;

import reka.ApplicationManager;
import reka.api.Path;
import reka.api.data.Data;
import reka.api.flow.FlowSegment;
import reka.configurer.annotations.Conf;
import reka.core.util.StringWithVars;

public class RekaDetailsConfigurer implements Supplier<FlowSegment> {
	
	private final ApplicationManager manager;
	
	private Path out = dots("app");
	private Function<Data,String> appFn;
	
	@Conf.Val
	public void out(String val) {
		out = dots(val);
	}
	
	@Conf.At("app")
	public void app(String val) {
		appFn = StringWithVars.compile(val);
	}
	
	public RekaDetailsConfigurer(ApplicationManager manager) {
		this.manager = manager;
	}
	
	@Override
	public FlowSegment get() {
		return sync("get app", (data) -> new RekaDetailsOperation(manager, appFn, out));
	}

}
