package reka.jruby;

import static reka.api.Path.root;
import static reka.config.configurer.Configurer.Preconditions.checkConfig;
import reka.api.IdentityKey;
import reka.config.Config;
import reka.config.configurer.annotations.Conf;
import reka.module.setup.AppSetup;
import reka.module.setup.ModuleConfigurer;

public class JRubyConfigurer extends ModuleConfigurer {
	
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
	public void setup(AppSetup app) {
		app.onDeploy(init -> {
			init.run("initialize runtime", () -> {
				RubyEnv env = RubyEnv.create(gemFile);
				env.exec(script);
				app.ctx().put(RUBY_ENV, env);
			});
		});
		app.defineOperation(root(), provider -> new JRubyRunConfigurer(app.path()));
	}
	
}