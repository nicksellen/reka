package reka.nashorn;

import static reka.api.Path.dots;
import static reka.nashorn.NashornConfigurer.RUNNER;
import reka.api.Path;
import reka.config.Config;
import reka.config.configurer.annotations.Conf;
import reka.core.setup.OperationConfigurer;
import reka.core.setup.OperationSetup;

public class NashornRunConfigurer implements OperationConfigurer {
	
	private String script;
	private Path out;
	
	public NashornRunConfigurer(Path defaultWriteTo) {
		this.out = defaultWriteTo;
	}

	@Conf.Val
	@Conf.At("out")
	@Conf.At("into")
	public void out(String val) {
		out = dots(val);
	}
	
	@Conf.Config
	@Conf.At("script")
	public void config(Config config) {
		if (config.hasDocument()) {
			script = config.documentContentAsString();
			if (config.hasValue()) {
				out = dots(config.valueAsString());
			}
		} else if (config.hasValue()) {
			script = config.valueAsString();
		}
	}

	@Override
	public void setup(OperationSetup ops) {
		ops.add("run", store -> new NashornRunOperation(store.get(RUNNER), script, out));
	}

}
