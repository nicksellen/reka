package reka.jruby;

import static reka.jruby.JRubyConfigurer.RUBY_ENV;
import static reka.util.Path.dots;
import reka.config.Config;
import reka.config.configurer.annotations.Conf;
import reka.module.setup.OperationConfigurer;
import reka.module.setup.OperationSetup;
import reka.util.Path;

public class JRubyRunConfigurer implements OperationConfigurer {
	
	private String script;
	private Path out;
	
	public JRubyRunConfigurer(Path defaultWriteTo) {
		this.out = defaultWriteTo;
	}
	
	@Conf.At("out")
	@Conf.At("into")
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
		ops.add("run", () -> new JRubyRunOperation(ops.ctx().get(RUBY_ENV), script, out));
	}
	
}