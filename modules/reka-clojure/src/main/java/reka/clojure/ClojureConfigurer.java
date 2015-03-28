package reka.clojure;

import static java.lang.String.format;
import static reka.api.Path.root;
import static reka.config.configurer.Configurer.Preconditions.checkConfig;
import static reka.util.Util.runtime;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.Identity;
import reka.api.IdentityKey;
import reka.api.data.MutableData;
import reka.config.Config;
import reka.config.ConfigBody;
import reka.config.configurer.annotations.Conf;
import reka.core.data.memory.MutableMemoryData;
import reka.core.setup.ModuleConfigurer;
import reka.core.setup.ModuleSetup;

import com.google.common.io.Resources;

public class ClojureConfigurer extends ModuleConfigurer {

	private final Logger log = LoggerFactory.getLogger(getClass());
	
	protected static final IdentityKey<ClojureEnv> CLOJURE_ENV = IdentityKey.named("clojure env");
	protected static final IdentityKey<List<Runnable>> SHUTDOWN_CALLBACKS = IdentityKey.named("shutdown callbacks");
	
	private final static String REKA_CLJ;
	
	static {
		try {
			REKA_CLJ = Resources.toString(Resources.getResource(ClojureConfigurer.class, "/reka.clj"), StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	private final Map<String,ConfigBody> triggerFns = new HashMap<>(); 
	
	public static void main(String[] args) throws InterruptedException {
		
		ClojureEnv a = ClojureEnv.create(ClojureConfigurer.class.getClassLoader());
		ClojureEnv b = ClojureEnv.create(ClojureConfigurer.class.getClassLoader());
		
		a.eval("(ns nick)\n(defn doit [m] (println (into {} m)))");
		b.eval("(ns nick)\n(defn doit [m] (println \"alex\"))");
		
		MutableData data = MutableMemoryData.create();
		data.putString("name", "james");
		a.run("nick/doit", data.viewAsMap());
		b.run("nick/doit", data.viewAsMap());
		
		a.shutdown();
		b.shutdown();
		
	}
	
	private String script;
	
	@Conf.Config
	@Conf.At("script")
	public void config(Config config) {
		if (config.hasDocument()) {
			script = config.documentContentAsString();
		}
	}
	
	@Conf.Each("defn")
	public void defn(Config config) {
		checkConfig(config.hasValue(), "must have a value");
		triggerFns.put(config.valueAsString(), config.body());
	}
	
	private final Map<Identity,ClojureEnv> envs = new HashMap<>();
	private final Map<Identity,Integer> versions = new HashMap<>();
	
	@Override
	public void setup(ModuleSetup app) {
		
		app.onDeploy(init -> {
		
			init.run("initialize environment", (idv, store) -> {

				versions.put(idv.identity(), idv.version());
				
				envs.computeIfPresent(idv.identity(), (id, env) -> {
					// this is doing a file based refresh I don't know
					// whether it'll work for me but it's a start...
					log.info("refreshing existing clojure env for {}", id);
					env.run("reka/do-refresh");
					return env;
				});
				
				ClojureEnv env = envs.computeIfAbsent(idv.identity(), id -> {
					log.info("creating new clojure env for {}", id);
					return ClojureEnv.create(ClojureConfigurer.class.getClassLoader());
				});

				env.eval(REKA_CLJ);
				
				List<Runnable> shutdownCallbacks = new ArrayList<>();
				
				env.run("reka/set-shutdown-collector", (Consumer<Runnable>) f -> {
					shutdownCallbacks.add(f);
				});
				
				store.put(CLOJURE_ENV, env);
				store.put(SHUTDOWN_CALLBACKS, shutdownCallbacks);
	
				// forward declare all the callbacks so the initialization can refer to them (they'll be overwritten later..._
				triggerFns.forEach((name, body) -> {
					env.run("reka/define-callback", name, (Runnable) () -> {
						throw runtime("sorry you can't use callback %s during initialization", name);
					});
				});
				
				env.eval(script);
				
				envs.put(idv.identity(), env);
			});
		});
		
		app.onUndeploy("shutdown env", (idv, store) -> {
			/* work in progress...
			if (false) {

			//if (versions.get(idv.identity()) == idv.version()) {
				log.info("shutting down env for {} as {} == {}", idv.identity(), versions.get(idv.identity()), idv.version());
				store.remove(CLOJURE_ENV).ifPresent(env -> {
					store.remove(SHUTDOWN_CALLBACKS).ifPresent(callbacks -> {
						callbacks.forEach(Runnable::run);
					});
					env.shutdown();
				});
				
				envs.remove(idv.identity());
			//}
				
			}
			*/
			
		});
		
		triggerFns.forEach((name, body) -> {
			app.buildFlow(format("on %s", name), body, flow -> {
				app.ctx().get(CLOJURE_ENV).run("reka/define-callback", name, (Runnable) () -> {
					flow.run();
				});
			});
		});
		
		app.defineOperation(root(), provider -> new ClojureRunConfigurer());
	}
	
}