package reka.clojure;

import java.io.StringReader;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import clojure.java.api.Clojure;
import clojure.lang.Compiler;
import clojure.lang.RT;

public class ClojureEnvImpl implements ClojureEnv {
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	private final ExecutorService executor;
	
	private final CountDownLatch latch = new CountDownLatch(1);
	
	public ClojureEnvImpl() {
		try {
			
			executor = Executors.newCachedThreadPool(new ThreadFactory(){

				@Override
				public Thread newThread(Runnable r) {
					Thread t = new Thread(r);
					t.setContextClassLoader(RT.class.getClassLoader());
					t.setDaemon(true);
					return t;
				}
				
			});

			executor.execute(() -> {
				try {
					log.info("initializing clojure");
					Class.forName("clojure.java.api.Clojure");
					log.info("clojure initialized :)"); 
				} catch (Exception e) {
					throw new RuntimeException(e);
				} finally {
					latch.countDown();
				}
			});

			latch.await();
			
		} catch (Throwable t) {
			throw new RuntimeException(t);
		}
	}
	
	@Override
	public void eval(String code) {
		CountDownLatch latch = new CountDownLatch(1);
		executor.execute(() -> {
			Compiler.load(new StringReader(code));
			latch.countDown();
		});
		try {
			latch.await();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public void run(String ns, String fn, Object arg) {
		executor.execute(() -> {
			try {
				Clojure.var(ns, fn).invoke(arg);
			} catch (IllegalArgumentException e) {
				throw new RuntimeException(e);
			}
		});
	}

	@Override
	public void shutdown() {
		executor.execute(() -> Thread.currentThread().setContextClassLoader(null));
		executor.shutdown();
		
	}
}