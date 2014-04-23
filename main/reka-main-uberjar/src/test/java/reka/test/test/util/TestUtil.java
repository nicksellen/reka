package reka.test.test.util;

import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestUtil {
	
	private static final Logger log = LoggerFactory.getLogger(TestUtil.class);

	public static void timed(final int warmup, final int times, int threads, final Runnable runnable) {
		
		final CountDownLatch startLatch = new CountDownLatch(threads);
		final CountDownLatch finishLatch = new CountDownLatch(threads);
		
		for (int t = 0; t < threads; t++) {

			new Thread() { 
			
				@Override
				public void run() {
				
					for (int i = 0; i < warmup; i++) {
						runnable.run();
					}
					
					startLatch.countDown();
					try {
						startLatch.await();
					} catch (InterruptedException e) { }
					
					for (int i = 0; i < times; i++) {
						runnable.run();
					}
					
					finishLatch.countDown();
				}
			}.start();
		}
		try {
			startLatch.await();
		} catch (InterruptedException e) { }
		
		long start = System.nanoTime();
		
		try {
			finishLatch.await();
		} catch (InterruptedException e) { }
		
		long finish = System.nanoTime();
		long durationMicroS = (finish - start) / 1000;
		long durationMS = (finish - start) / 1000000;
		long totalTimes = times * threads;
		double each = new Double(durationMicroS) / new Double(totalTimes);
		double perSecond = new Double(totalTimes) / (new Double(durationMS) / 1000);
		log.debug("took %ds for %d iterations (%d threads) %.4f microseconds/iteration %.1f iterations/second\n", durationMS, totalTimes, threads, each, perSecond);
	}
	
}
