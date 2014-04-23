package reka.nashorn;

import static reka.api.Path.dots;
import static reka.core.builder.FlowSegments.sync;

import java.util.function.Supplier;

import javax.script.ScriptEngine;

import reka.api.Path;
import reka.api.flow.FlowSegment;
import reka.config.Config;
import reka.configurer.annotations.Conf;

public class NashornRunConfigurer implements Supplier<FlowSegment> {
	
	private final Path runtimePath;
	
	private String script;
	private Path out;
	
	public NashornRunConfigurer(Path runtimePath, Path defaultWriteTo) {
		this.runtimePath = runtimePath;
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
		
		return sync("run", (data) -> new NashornRunOperation(
				data.getContent(runtimePath).get().valueAs(ScriptEngine.class), script, out));
	}

}
