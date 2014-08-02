package reka.rhino;

import static reka.api.Path.dots;
import static reka.api.Path.root;
import static reka.core.builder.FlowSegments.sync;
import static reka.rhino.RhinoHelper.compileJavascript;

import java.util.function.Supplier;

import org.mozilla.javascript.Script;
import org.mozilla.javascript.ScriptableObject;

import reka.api.Path;
import reka.api.flow.FlowSegment;
import reka.config.Config;
import reka.configurer.annotations.Conf;

public class RhinoConfigurer implements Supplier<FlowSegment> {
	
	private final Path scopePath;
	
	private Script script;
	private Path out = root();
	
	public RhinoConfigurer(Path scopePath) {
		this.scopePath = scopePath;
	}
	
	@Conf.Config
	@Conf.At("script")
	public void config(Config config) {
		if (config.hasDocument()) {
			script = compileJavascript(config.documentContentAsString());
			if (config.hasValue()) {
				out = dots(config.valueAsString());
			}
		} else if (config.hasValue()) {
			script = compileJavascript(config.valueAsString());
		}
	}
	
	@Conf.At("out")
	public void out(String val) {
		out = dots(val);
	}
	
	@Override
	public FlowSegment get() {
		return sync("run(javascript)", (data) -> {
			return new RhinoWithScope(
					data.getContent(scopePath).get().valueAs(ScriptableObject.class), 
					script, 
					out);
		});
	}

}
