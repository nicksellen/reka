package reka.admin;

import static reka.api.Path.dots;
import static reka.core.builder.FlowSegments.sync;

import java.util.function.Supplier;

import reka.ApplicationManager;
import reka.api.Path;
import reka.api.flow.FlowSegment;
import reka.config.configurer.annotations.Conf;

public class RekaListConfigurer implements Supplier<FlowSegment> {
	
	private final ApplicationManager manager;
	
	private Path out = dots("apps");
	
	@Conf.Val
	public void out(String val) {
		out = dots(val);
	}
	
	public RekaListConfigurer(ApplicationManager manager) {
		this.manager = manager;
	}
	
	@Override
	public FlowSegment get() {
		return sync("list apps", () -> new RekaListOperation(manager, out));
	}

}
