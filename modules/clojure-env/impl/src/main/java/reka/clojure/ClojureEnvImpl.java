package reka.clojure;

import static clojure.java.api.Clojure.var;

import java.io.StringReader;
import java.net.URLClassLoader;

import clojure.lang.Compiler;
import clojure.lang.RT;

public class ClojureEnvImpl implements ClojureEnv {
	
	public ClojureEnvImpl() {
		env(() -> Class.forName("clojure.java.api.Clojure"));
	}
	
	private static interface ThrowingRunnable {
		void run() throws Exception;
	}
	
	@Override
	public void eval(String code) {
		env(() -> Compiler.load(new StringReader(code)));
	}
	
	@Override
	public void run(String namespacedFn) {
		env(() -> var(namespacedFn).invoke());
	}
	
	@Override
	public void run(String namespacedFn, Object arg1) {
		env(() -> var(namespacedFn).invoke(arg1));
	}

	@Override
	public void run(String namespacedFn, Object arg1, Object arg2) {
		env(() -> var(namespacedFn).invoke(arg1, arg2));
	}

	@Override
	public void shutdown() {
		try {
			ClassLoader cl = RT.class.getClassLoader();
			if (cl instanceof URLClassLoader) {
				((URLClassLoader) cl).close();
			}
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}
	
	private void env(ThrowingRunnable r) {
		Thread t = Thread.currentThread();
		ClassLoader cl = t.getContextClassLoader();
		t.setContextClassLoader(RT.class.getClassLoader());
		try {
			r.run();
		} catch (Exception e) {
			e.printStackTrace();
			// ignore
		} finally {
			t.setContextClassLoader(cl);
		}
	}
}