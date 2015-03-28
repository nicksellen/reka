package reka.rhino;

import static reka.api.Path.root;
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

import reka.api.IdentityKey;
import reka.config.Config;
import reka.config.configurer.annotations.Conf;
import reka.core.setup.ModuleConfigurer;
import reka.core.setup.ModuleSetup;

public class RhinoConfigurer extends ModuleConfigurer {
	
	protected static final IdentityKey<ScriptableObject> SCOPE = IdentityKey.named("scope");
	
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
		} else if (config.hasValue()) {
			scripts.add(compileJavascript(config.valueAsString(), optimization));
		}
	}

	@Override
	public void setup(ModuleSetup module) {
		
		module.onDeploy(init -> {
		
			init.run("create js scope", ctx -> {
				Context context = Context.enter();
				if (optimization != null) context.setOptimizationLevel(optimization);
				try {
					ctx.put(SCOPE, context.initStandardObjects(null, false));
				} finally {
					Context.exit();
				}
			});
	
			log.info("setting up {} script(s)", scripts.size());
			
			for (Script script : scripts) {
				init.run("run initial javascript", ctx -> {
					log.debug("running initial js");
					runJavascriptInScope(ctx.get(SCOPE), script, optimization);
				});
			}
		});
		
		module.defineOperation(root(), provider -> new RhinoRunConfigurer());
		
	}

}
