package reka.nashorn;

import static java.util.Arrays.asList;
import static reka.api.Path.root;
import static reka.api.content.Contents.nonSerializableContent;
import static reka.util.Util.unchecked;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import reka.api.Path;
import reka.config.Config;
import reka.configurer.annotations.Conf;
import reka.core.bundle.UseConfigurer;
import reka.core.bundle.UseInit;

public class UseNashorn extends UseConfigurer {
	
	String script;
	
	@Conf.Config
	public void config(Config config) {
		if (config.hasDocument()) {
			script = config.documentContentAsString();
		}
	}

	@Override
	public void setup(UseInit init) {
		
		Path runtimePath = init.path().add("runtime");
		
		init.run("setup", (data) -> {
			
			ScriptEngineManager factory = new ScriptEngineManager();
	        ScriptEngine engine = factory.getEngineByName("nashorn");
	        
	        if (script != null) {
	        	try {
	        		//engine.
					engine.eval(script);
				} catch (Exception e) {
					throw unchecked(e);
				}
	        }
	        
	        data.put(runtimePath, nonSerializableContent(engine));
			
			return data;
		});
		
		init.operation(asList("run", ""), () -> new NashornRunConfigurer(runtimePath, root()));
		
	}

}
