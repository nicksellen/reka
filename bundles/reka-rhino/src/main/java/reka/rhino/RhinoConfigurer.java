package reka.rhino;

import static reka.api.Path.dots;
import static reka.api.Path.root;
import static reka.core.builder.FlowSegments.storeSync;
import static reka.rhino.RhinoHelper.compileJavascript;
import static reka.rhino.RhinoModule.SCOPE;

import java.util.function.Supplier;

import org.mozilla.javascript.Script;

import reka.api.Path;
import reka.api.flow.FlowSegment;
import reka.config.Config;
import reka.config.configurer.annotations.Conf;

public class RhinoConfigurer implements Supplier<FlowSegment> {
	
	private Script script;
	private Path out = root();
	
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
		return storeSync("run", store -> new RhinoWithScope(store.get(SCOPE), script, out));
	}

}
