package reka.jruby;

import static java.util.Arrays.asList;
import static reka.api.Path.path;
import static reka.api.Path.root;
import static reka.api.content.Contents.nonSerializableContent;

import org.jruby.embed.LocalContextScope;
import org.jruby.embed.LocalVariableBehavior;
import org.jruby.embed.ScriptingContainer;

import reka.api.Path;
import reka.config.Config;
import reka.config.configurer.annotations.Conf;
import reka.core.bundle.ModuleConfigurer;
import reka.core.bundle.ModuleInit;

public class JRubyModule extends ModuleConfigurer {

	private String script;
	
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
	
	@Override
	public void setup(ModuleInit init) {
		
		Path runtimePath = init.path().add("runtime");
		
		init.init("initialize runtime", (data) -> {
			ScriptingContainer container = new ScriptingContainer(
					LocalContextScope.SINGLETHREAD,
					LocalVariableBehavior.TRANSIENT);
			container.runScriptlet(script);
			data.put(runtimePath, nonSerializableContent(container));
			return data;
		});
		
		init.operation(asList(path("run"), root()), () -> new JRubyRunConfigurer(runtimePath, init.path()));
		
	}
	
}