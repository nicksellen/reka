package reka.jruby;

import static reka.api.Path.dots;
import static reka.jruby.JRubyModule.RUBY_ENV;
import reka.api.Path;
import reka.config.Config;
import reka.config.configurer.annotations.Conf;
import reka.core.setup.OperationSetup;
import reka.nashorn.OperationConfigurer;

public class JRubyRunConfigurer implements OperationConfigurer {
	
	private String script;
	private Path out;
	
	public JRubyRunConfigurer(Path defaultWriteTo) {
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
	public void setup(OperationSetup ops) {
		ops.add("run", store -> new JRubyRunOperation(store.get(RUBY_ENV), script, out));
	}
	
}