package reka.jruby;

import static reka.api.Path.root;
import static reka.config.configurer.Configurer.Preconditions.checkConfig;
import reka.api.IdentityKey;
import reka.config.Config;
import reka.config.configurer.annotations.Conf;
import reka.core.setup.ModuleConfigurer;
import reka.core.setup.ModuleSetup;

public class JRubyModule extends ModuleConfigurer {
	
	protected static final IdentityKey<RubyEnv> RUBY_ENV = IdentityKey.named("ruby env");
	
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
	public void setup(ModuleSetup module) {
		module.setupInitializer(init -> {
			init.run("initialize runtime", store -> {
				RubyEnv env = RubyEnv.create(gemFile);
				env.exec(script);
				store.put(RUBY_ENV, env);
			});
		});
		module.operation(root(), provider -> new JRubyRunConfigurer(module.path()));
	}
	
}