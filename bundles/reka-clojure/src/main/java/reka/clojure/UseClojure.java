package reka.clojure;

import static reka.api.Path.root;

import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicReference;

import reka.api.data.MutableData;
import reka.config.Config;
import reka.config.configurer.annotations.Conf;
import reka.core.bundle.UseConfigurer;
import reka.core.bundle.UseInit;
import reka.core.data.memory.MutableMemoryData;

public class UseClojure extends UseConfigurer {

	private final static WeakHashMap<ClojureEnv, String> envs = new WeakHashMap<>();
	
	public static void main(String[] args) throws InterruptedException {
		
		ClojureEnv a = ClojureEnv.create();
		ClojureEnv b = ClojureEnv.create();
		
		a.eval("(ns nick)\n(defn doit [m] (println (into {} m)))");
		b.eval("(ns nick)\n(defn doit [m] (println \"alex\"))");
		
		MutableData data = MutableMemoryData.create();
		data.putString("name", "james");
		a.run("nick", "doit", data.viewAsMap());
		b.run("nick", "doit", data.viewAsMap());
		
		Thread.sleep(1000);
	}
	
	private String script;
	
	@Conf.Config
	public void config(Config config) {
		if (config.hasDocument()) {
			script = config.documentContentAsString();
		}
	}
	
	@Override
	public void setup(UseInit init) {
		
		AtomicReference<ClojureEnv> runtimeRef = new AtomicReference<>();
		
		init.run("initialize environment", (data) -> {
			runtimeRef.set(ClojureEnv.create());
			runtimeRef.get().eval(script);
			envs.put(runtimeRef.get(), "");
			System.out.printf("there are %d clojure envs in memory\n", envs.size());
			return data;
		});
		
		init.shutdown("shutdown env", () -> {
			runtimeRef.get().shutdown();
			runtimeRef.set(null);
		});
		
		init.operation(root(), () -> new ClojureRunConfigurer(runtimeRef));
	}
	
}