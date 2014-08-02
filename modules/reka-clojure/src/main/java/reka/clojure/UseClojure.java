package reka.clojure;

import static java.util.Arrays.asList;
import static reka.util.Util.unchecked;

import java.io.StringReader;

import reka.config.Config;
import reka.configurer.annotations.Conf;
import reka.core.bundle.UseConfigurer;
import reka.core.bundle.UseInit;
import clojure.lang.Compiler;
import clojure.lang.RT;

public class UseClojure extends UseConfigurer {
	
	private String script;
	
	@Conf.Config
	public void config(Config config) {
		if (config.hasDocument()) {
			script = config.documentContentAsString();
		}
	}
	
	@Override
	public void setup(UseInit init) {
		
		init.run("initialize environment", (data) -> {
			
			try {
				
				// TODO: this only allows one clojure runtime per vm, not good enough!
				// Clojure 1.6 is promising something nicer to handle this...
				
				RT.load("clojure/core");
				Compiler.load(new StringReader(script));
				
			} catch (Exception e) {
				throw unchecked(e);
			}
			
			return data;
		});
		
		init.operation(asList(""), () -> new ClojureRunConfigurer());
	}
	
}