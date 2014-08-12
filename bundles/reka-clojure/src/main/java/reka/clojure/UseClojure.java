package reka.clojure;

import static reka.api.Path.root;
import static reka.config.configurer.Configurer.Preconditions.checkConfig;
import static reka.util.Util.runtime;
import static reka.util.Util.unchecked;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicReference;

import reka.api.data.MutableData;
import reka.config.Config;
import reka.config.ConfigBody;
import reka.config.configurer.annotations.Conf;
import reka.core.bundle.UseConfigurer;
import reka.core.bundle.UseInit;
import reka.core.data.memory.MutableMemoryData;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;

public class UseClojure extends UseConfigurer {

	private final static WeakHashMap<ClojureEnv, String> envs = new WeakHashMap<>();
	
	private final Map<String,ConfigBody> triggerFns = new HashMap<>(); 
	
	public static void main(String[] args) throws InterruptedException {
		
		ClojureEnv a = ClojureEnv.create(UseClojure.class.getClassLoader());
		ClojureEnv b = ClojureEnv.create(UseClojure.class.getClassLoader());
		
		a.eval("(ns nick)\n(defn doit [m] (println (into {} m)))");
		b.eval("(ns nick)\n(defn doit [m] (println \"alex\"))");
		
		MutableData data = MutableMemoryData.create();
		data.putString("name", "james");
		a.run("nick", "doit", data.viewAsMap());
		b.run("nick", "doit", data.viewAsMap());
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
	
	@Override
	public void setup(UseInit init) {
		
		AtomicReference<ClojureEnv> runtimeRef = new AtomicReference<>();
		
		init.run("initialize environment", (data) -> {
			
			ClojureEnv env = ClojureEnv.create(UseClojure.class.getClassLoader());

			try {
				String rekaClj = Resources.toString(Resources.getResource(UseClojure.class, "/reka.clj"), Charsets.UTF_8);
				
				env.eval(rekaClj);
				
			} catch (IOException e) {
				throw unchecked(e);
			}

			// forward declare all the callbacks so the initialization can refer to them (they'll be overwritten later..._
			triggerFns.forEach((name, body) -> {
				env.run("reka/define-callback", name, (Runnable) () -> {
					throw runtime("sorry you can't use callback %s during initialization", name);
				});
			});
			
			env.eval(script);
			envs.put(env, "");
			
			runtimeRef.set(env);
			
			return data;
		});
		
		init.shutdown("shutdown env", () -> {
			runtimeRef.get().shutdown();
			runtimeRef.set(null);
		});
		
		triggerFns.forEach((name, body) -> {
			init.trigger(name, body, registration -> {
				runtimeRef.get().run("reka/define-callback", name, (Runnable) () -> {
					registration.flow().run();
				});
			});
		});
		
		init.operation(root(), () -> new ClojureRunConfigurer(runtimeRef));
	}
	
}