package reka.rhino;

import static reka.api.Path.dots;
import static reka.api.Path.root;
import static reka.rhino.RhinoHelper.compileJavascript;
import static reka.rhino.RhinoModule.SCOPE;

import org.mozilla.javascript.Script;

import reka.api.Path;
import reka.config.Config;
import reka.config.configurer.annotations.Conf;
import reka.core.setup.OperationSetup;
import reka.nashorn.OperationConfigurer;

public class RhinoConfigurer implements OperationConfigurer {
	
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
	public void setup(OperationSetup ops) {
		ops.add("run", store -> new RhinoWithScope(store.get(SCOPE), script, out));
	}

}
