package reka.nashorn;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static reka.api.Path.path;
import static reka.api.Path.root;
import static reka.config.configurer.Configurer.Preconditions.checkConfig;

import java.util.ArrayList;
import java.util.List;

import reka.config.Config;
import reka.config.configurer.annotations.Conf;
import reka.core.bundle.ModuleConfigurer;
import reka.core.bundle.ModuleInit;

public class NashornModule extends ModuleConfigurer {
	
	List<String> scripts = new ArrayList<>();
	
	@Conf.Config
	public void config(Config config) {
		if (config.hasDocument()) {
			scripts.add(config.documentContentAsString());
		}
	}
	
	@Conf.Each("script")
	public void script(Config config) {
		checkConfig(config.hasDocument(), "must have document");
		scripts.add(config.documentContentAsString());
	}
	
	@Conf.Each("var")
	public void var(Config config) {
		checkConfig(config.hasKey() && config.hasValue() && config.hasDocument(), "must have key/value/document");
		String varname = config.valueAsString().replaceAll(" ", "_");
		String escaped = config.documentContentAsString().replace("\"", "\\\"").replace("\n", "\\n");
		scripts.add(0, format("var %s = \"%s\";", varname, escaped));
	}

	@Override
	public void setup(ModuleInit init) {
		

		ThreadLocal<NashornRunner> runner = new ThreadLocal<NashornRunner>(){

			@Override
			protected NashornRunner initialValue() {
				return new NashornRunner(scripts);
			}
			
		};
		
		init.operation(asList(path("run"), root()), () -> new NashornRunConfigurer(runner, root()));
	}

}
