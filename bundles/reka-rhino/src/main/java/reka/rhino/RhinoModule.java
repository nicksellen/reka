package reka.rhino;

import static java.util.Arrays.asList;
import static reka.api.Path.path;
import static reka.api.Path.root;
import static reka.api.content.Contents.nonSerializableContent;
import static reka.rhino.RhinoHelper.compileJavascript;
import static reka.rhino.RhinoHelper.runJavascriptInScope;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.ScriptableObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.api.Path;
import reka.config.Config;
import reka.config.configurer.annotations.Conf;
import reka.core.bundle.ModuleConfigurer;
import reka.core.bundle.ModuleInit;

import com.google.common.base.Charsets;

public class RhinoModule extends ModuleConfigurer {
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	private final List<Script> scripts = new ArrayList<>();
	
	private Integer optimization;

	@Conf.Config
	public void script1(Config config) {
		if (config.hasDocument()) {
			scripts.add(compileJavascript(config.documentContentAsString()));
		}
	}
	
	@Conf.At("optimization")
	public void optimization(BigDecimal val) {
		optimization = val.intValue();
	}
	
	@Conf.Each("script")
	public void script2(Config config) {
		if (config.hasDocument()) {
			scripts.add(compileJavascript(config.documentContentAsString(), optimization));
		} else if (config.hasData()) {
			scripts.add(compileJavascript(new String(config.data(), Charsets.UTF_8), optimization));
		} else if (config.hasValue()) {
			scripts.add(compileJavascript(config.valueAsString(), optimization));
		}
	}

	@Override
	public void setup(ModuleInit module) {
		
		Path scopePath = module.path().add("scope");
		
		module.init("create js scope", (data) -> {
			Context context = Context.enter();
			if (optimization != null) context.setOptimizationLevel(optimization);
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

		log.info("setting up {} script(s)", scripts.size());
		
		for (Script script : scripts) {
			module.init("run initial javascript", (data) -> {
				log.debug("running initial js");
				ScriptableObject scope = data.getContent(scopePath).get().valueAs(ScriptableObject.class);
				runJavascriptInScope(scope, script, optimization);
				log.debug("returning data from js init/run: {}", data.toPrettyJson());
				return data;
			});
		}
		
		module.operation(asList(root(), path("run")), () -> new RhinoConfigurer(scopePath));
		
	}

}
