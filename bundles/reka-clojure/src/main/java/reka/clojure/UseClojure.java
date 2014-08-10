package reka.clojure;

import static reka.api.Path.root;
import static reka.util.Util.unchecked;

import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.WeakHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import reka.api.data.Data;
import reka.api.data.MutableData;
import reka.config.Config;
import reka.config.configurer.annotations.Conf;
import reka.core.bundle.UseConfigurer;
import reka.core.bundle.UseInit;
import reka.core.data.memory.MutableMemoryData;

public class UseClojure extends UseConfigurer {

	private final static WeakHashMap<ClojureEnv, String> envs = new WeakHashMap<>();
	
	public static class ClojureEnv implements Runnable {

		private final String init;
		private final ClassLoader cl;
		private Class<?> compiler;
		private Class<?> rt;
		private Class<?> ifn;
		private Method compiler_load;
		private Method rt_load;
		private Method rt_var;
		private Method ifn_invoke;
		
		private final Executor executor;
		
		private final CountDownLatch latch = new CountDownLatch(1);
		
		public ClojureEnv(String init) {
			try {
				this.init = init;
				URL[] urls = { new URL("file:/Users/nick/.m2/repository/org/clojure/clojure/1.6.0/clojure-1.6.0.jar") };
				cl = new URLClassLoader(urls, UseClojure.class.getClassLoader());
				
				executor = Executors.newSingleThreadExecutor(new ThreadFactory(){

					@Override
					public Thread newThread(Runnable r) {
						Thread t = new Thread(r);
						t.setContextClassLoader(cl);
						return t;
					}
					
				});
				
				executor.execute(this);
				
				latch.await();
				
			} catch (Throwable t) {
				throw unchecked(t);
			}
		}


		@Override
		public void run() {			
			try {
				rt = cl.loadClass("clojure.lang.RT");
				compiler = cl.loadClass("clojure.lang.Compiler");
				ifn = cl.loadClass("clojure.lang.IFn");
				
				rt_load = rt.getMethod("load", String.class);
				compiler_load = compiler.getMethod("load", Reader.class);
				rt_var = rt.getMethod("var", String.class, String.class);
				rt_load.invoke(rt, "clojure/core");
				compiler_load.invoke(compiler, new StringReader(init));
				//compiler_load.invoke(compiler, new StringReader("(defn from-map [m] (into {} m))"));
				ifn_invoke = ifn.getMethod("invoke", Object.class);
				latch.countDown();
			} catch (Throwable t) {
				throw unchecked(t);
			}
		}
		
		/*
		
		public void boo(String ns, String fn, Consumer<Consumer<Data>> c) {
			executor.execute(() -> {
				try {
					Object fnref = rt_var.invoke(rt, ns, fn);
					c.accept(data -> {
						try {
							ifn_invoke.invoke(fnref, data);
						} catch (Exception e) {
							throw unchecked(e);
						}
					});
				} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
					throw unchecked(e);
				}
			});
		}
		*/
		
		public void run(String ns, String fn, Object... args) {
			executor.execute(() -> {
				try {
					ifn_invoke.invoke(rt_var.invoke(rt, ns, fn), args);
				} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
					throw unchecked(e);
				}
			});
		}
	}
	
	public static void main(String[] args) {
		/*
		ClojureRuntimeShim runtime = ClojureRuntimeShim.newRuntime(cl, init.path().slashes());
		*/
		
	//Object retval = runtime.invoke("my-app.core/some-fn", arg1, arg2);
		
		//runtimeRef.set(runtime);
		
		ClojureEnv a = new ClojureEnv("(ns nick)\n(defn doit [m] (println (into {} m)))");
		//ClojureEnv b = new ClojureEnv("(ns nick)\n(defn boo [] (println \"alex\"))");
		
		MutableData data = MutableMemoryData.create();
		data.putString("name", "james");
		a.run("nick", "boo", data.viewAsMap());
		
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
			ClojureEnv env = new ClojureEnv(script);
			envs.put(env, "boooop");
			System.out.printf("there are %d clojure envs in memory\n", envs.size());
			System.gc();
			runtimeRef.set(env);
			return data;
		});
		
		init.operation(root(), () -> new ClojureRunConfigurer(runtimeRef));
	}
	
}