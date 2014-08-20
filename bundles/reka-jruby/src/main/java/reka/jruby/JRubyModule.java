package reka.jruby;

import static java.util.Arrays.asList;
import static reka.api.Path.path;
import static reka.api.Path.root;
import static reka.api.content.Contents.nonSerializableContent;
import static reka.config.configurer.Configurer.Preconditions.checkConfig;
import reka.api.Path;
import reka.config.Config;
import reka.config.configurer.annotations.Conf;
import reka.core.bundle.ModuleConfigurer;
import reka.core.bundle.ModuleInit;

public class JRubyModule extends ModuleConfigurer {
	
	private String script;
	private String gemFile;
	
	@Conf.Config
	public void config(Config config) {
		if (config.hasDocument()) {
			script = config.documentContentAsString();
		}
	}
	
	@Conf.At("init")
	public void init(Config config) {
		if (config.hasDocument()) {
			script = config.documentContentAsString();
		}
	}
	
	@Conf.At("Gemfile")
	public void gemfile(Config config) {
		checkConfig(config.hasDocument(), "must have document");
		gemFile = config.documentContentAsString();
	}
	
	@Override
	public void setup(ModuleInit module) {
		
		Path runtimePath = module.path().add("runtime");
		
		module.init("initialize runtime", (data) -> {
			RubyEnv env = RubyEnv.create(gemFile);
			env.exec(script);
			data.put(runtimePath, nonSerializableContent(env));
			return data;
		});
		
		module.operation(asList(path("run"), root()), () -> new JRubyRunConfigurer(runtimePath, module.path()));
	}
	
}