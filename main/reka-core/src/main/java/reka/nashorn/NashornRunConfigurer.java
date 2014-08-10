package reka.nashorn;

import static reka.api.Path.dots;
import static reka.core.builder.FlowSegments.sync;

import java.util.function.Supplier;

import reka.api.Path;
import reka.api.flow.FlowSegment;
import reka.config.Config;
import reka.config.configurer.annotations.Conf;

public class NashornRunConfigurer implements Supplier<FlowSegment> {
	
	private final ThreadLocal<NashornRunner> runner;
	
	private String script;
	private Path out;
	
	public NashornRunConfigurer(ThreadLocal<NashornRunner> runner, Path defaultWriteTo) {
		this.runner = runner;
		this.out = defaultWriteTo;
	}

	@Conf.At("out")
	public void out(String val) {
		out = dots(val);
	}
	
	@Conf.Config
	public void config(Config config) {
		if (config.hasDocument()) {
			script = config.documentContentAsString();
		} else if (config.hasValue()) {
			script = config.valueAsString();
		}
	}

	@Override
	public FlowSegment get() {
		return sync("run", (data) -> new NashornRunOperation(runner, script, out));
	}

}
