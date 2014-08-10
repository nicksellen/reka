package reka.rhino;

import static java.util.Arrays.asList;
import static reka.api.Path.path;
import static reka.api.Path.root;
import static reka.api.content.Contents.nonSerializableContent;
import static reka.rhino.RhinoHelper.compileJavascript;
import static reka.rhino.RhinoHelper.runJavascriptInScope;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.ScriptableObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.api.Path;
import reka.config.Config;
import reka.config.configurer.annotations.Conf;
import reka.core.bundle.UseConfigurer;
import reka.core.bundle.UseInit;

import com.google.common.base.Charsets;

public class UseRhino extends UseConfigurer {
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	private Script script;

	@Conf.Config
	public void script1(Config config) {
		if (config.hasDocument()) {
			script = compileJavascript(config.documentContentAsString());
		}
	}
	
	@Conf.At("script")
	public void script2(Config config) {
		if (config.hasDocument()) {
			script = compileJavascript(config.documentContentAsString());
		} else if (config.hasData()) {
			script = compileJavascript(new String(config.data(), Charsets.UTF_8));
		} else if (config.hasValue()) {
			script = compileJavascript(config.valueAsString());
		}
	}

	@Override
	public void setup(UseInit init) {
		
		Path scopePath = init.path().add("scope");
		
		init.run("create js scope", (data) -> {
			Context context = Context.enter();
			try {
				ScriptableObject scope = context.initStandardObjects(null, false);
				data.put(scopePath, nonSerializableContent(scope));
				log.debug("put scope at [{}]", scopePath.slashes());
			} finally {
				Context.exit();
			}
			
			log.debug("returning data from js init: {}", data.toPrettyJson());
			
			return data;
		});
		
		if (script != null) {
			init.run("run initial javascript", (data) -> {
				log.debug("running initial js");
				ScriptableObject scope = data.getContent(scopePath).get().valueAs(ScriptableObject.class);
				runJavascriptInScope(scope, script);
				log.debug("returning data from js init/run: {}", data.toPrettyJson());
				return data;
			});
		}
		
		init.operation(asList(root(), path("run")), () -> new RhinoConfigurer(scopePath));
		
	}

}
