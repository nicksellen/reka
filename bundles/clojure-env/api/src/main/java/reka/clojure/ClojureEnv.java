package reka.clojure;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface ClojureEnv {
	
	static final Logger log = LoggerFactory.getLogger(ClojureEnv.class);
	
	public static ClojureEnv create() {
		try {
			log.info("creating clojure env");
			InputStream s = ClojureEnv.class.getClassLoader().getResourceAsStream("clojure-env-impl.jar");
			Path tmp = Files.createTempFile("clojure-env-impl", ".jar");
			Files.copy(s, tmp, StandardCopyOption.REPLACE_EXISTING);
			URL[] urls = { tmp.toUri().toURL() };
			log.info("making classloader");
			@SuppressWarnings("resource")			
			ClassLoader cl = new URLClassLoader(urls, ClojureEnv.class.getClassLoader());

			log.info("loading impl");
			
			@SuppressWarnings("unchecked")
			Class<ClojureEnv> klass = (Class<ClojureEnv>) cl.loadClass("reka.clojure.ClojureEnvImpl");
			
			log.info("instantiating instantce");
			return klass.newInstance();
		} catch (InstantiationException | IllegalAccessException | ClassNotFoundException | IOException e) {
			throw new RuntimeException(e);
		} finally {
			log.info("done!");
		}
	}

	void eval(String code);
	void run(String ns, String fn, Object arg);
	void shutdown();
}