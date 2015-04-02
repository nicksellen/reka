package reka.clojure;

import java.net.URL;
import java.net.URLClassLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface ClojureEnv {
	
	static final Logger log = LoggerFactory.getLogger(ClojureEnv.class);

	public static ClojureEnv create(ClassLoader parent) {
		try {
			
			URL[] urls = { ClojureEnvImplJar.url() };
						
			@SuppressWarnings("resource")
			ClassLoader cl = new URLClassLoader(urls, parent);
			
			@SuppressWarnings("unchecked")
			Class<ClojureEnv> klass = (Class<ClojureEnv>) cl.loadClass("reka.clojure.ClojureEnvImpl");
			
			return klass.newInstance();
		} catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
			throw new RuntimeException(e);
		} finally {
			log.info("done!");
		}
	}

	void eval(String code);
	void run(String namespacedFn);
	void run(String namespacedFn, Object arg1);
	void run(String namespacedFn, Object arg1, Object arg2);
	void shutdown();
	
}