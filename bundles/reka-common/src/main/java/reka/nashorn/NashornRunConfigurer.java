package reka.nashorn;

import static reka.api.Path.dots;
import static reka.nashorn.NashornModule.RUNNER;
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
		ops.add("run", store -> new NashornRunOperation(store.get(RUNNER), script, out));
	}

}
