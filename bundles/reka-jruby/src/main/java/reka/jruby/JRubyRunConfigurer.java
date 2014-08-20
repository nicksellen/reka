package reka.jruby;

import static reka.api.Path.dots;
import static reka.core.builder.FlowSegments.sync;

import java.util.function.Supplier;

import org.jruby.embed.ScriptingContainer;

import reka.api.Path;
import reka.api.flow.FlowSegment;
import reka.config.Config;
import reka.config.configurer.annotations.Conf;

public class JRubyRunConfigurer implements Supplier<FlowSegment> {
	
	private final Path runtimePath;
	
	private String script;
	private Path out;
	
	public JRubyRunConfigurer(Path runtimePath, Path defaultWriteTo) {
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
		
		return sync("run", (data) -> new JRubyRunOperation(
				data.getContent(runtimePath).get().valueAs(RubyEnv.class), script, out));
	}
	
}