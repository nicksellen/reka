package reka.jruby;

import static java.util.Arrays.asList;
import static reka.api.content.Contents.nonSerializableContent;

import org.jruby.embed.LocalContextScope;
import org.jruby.embed.LocalVariableBehavior;
import org.jruby.embed.ScriptingContainer;

import reka.api.Path;
import reka.config.Config;
import reka.config.configurer.annotations.Conf;
import reka.core.bundle.UseConfigurer;
import reka.core.bundle.UseInit;

public class UseJRuby extends UseConfigurer {

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
	public void setup(UseInit init) {
		
		Path runtimePath = init.path().add("runtime");
		
		init.run("initialize runtime", (data) -> {
			ScriptingContainer container = new ScriptingContainer(
					LocalContextScope.SINGLETHREAD,
					LocalVariableBehavior.TRANSIENT);
			container.runScriptlet(script);
			data.put(runtimePath, nonSerializableContent(container));
			return data;
		});
		
		init.operation(asList("run", ""), () -> new JRubyRunConfigurer(runtimePath, init.path()));
		
	}
	
}