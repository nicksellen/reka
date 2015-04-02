package reka.test.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.Test;

import reka.util.Util;

public class UtilTest {
	
	@Test
	public void canDetectSingleThreadExecutor() {
		ExecutorService executor = Executors.newSingleThreadExecutor();
		assertTrue(Util.isSingleThreaded(executor));
	}

	@Test
	public void canDetectWorkstealingParalellism1Executor() {
		ExecutorService executor = Executors.newWorkStealingPool(1);
		assertTrue(Util.isSingleThreaded(executor));
	}
	
	@Test
	public void canDetectWorkstealingParallelism2Executor() {
		ExecutorService executor = Executors.newWorkStealingPool(2);
		assertFalse(Util.isSingleThreaded(executor));
	}
	
	@Test
	public void canDetectFixedThreadPoolThreadedExecutor() {
		ExecutorService executor = Executors.newFixedThreadPool(2);
		assertFalse(Util.isSingleThreaded(executor));
	}
	
	@Test
	public void canDetectCachedThreadPoolExecutor() {
		ExecutorService executor = Executors.newCachedThreadPool();
		assertFalse(Util.isSingleThreaded(executor));
	}

}
