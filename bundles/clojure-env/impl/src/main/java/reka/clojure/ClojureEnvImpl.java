package reka.clojure;

import static clojure.java.api.Clojure.var;

import java.io.StringReader;

import clojure.lang.Compiler;
import clojure.lang.RT;

public class ClojureEnvImpl implements ClojureEnv {
	
	private final ClassLoader cl = RT.class.getClassLoader();
	
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
	public void run(String namespacedFn, Object arg1) {
		env(() -> var(namespacedFn).invoke(arg1));
	}

	@Override
	public void run(String namespacedFn, Object arg1, Object arg2) {
		env(() -> var(namespacedFn).invoke(arg1, arg2));
	}

	@Override
	public void shutdown() {
	}
	
	private void env(ThrowingRunnable r) {
		Thread t = Thread.currentThread();
		ClassLoader oldcl = t.getContextClassLoader();
		t.setContextClassLoader(cl);
		try {
			r.run();
		} catch (Exception e) {
			e.printStackTrace();
			// ignore
		} finally {
			t.setContextClassLoader(oldcl);
		}
	}
}