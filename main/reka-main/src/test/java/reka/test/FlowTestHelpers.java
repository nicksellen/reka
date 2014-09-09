package reka.test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import reka.api.data.MutableData;
import reka.api.flow.FlowOperation;
import reka.api.run.AsyncOperation;
import reka.api.run.Operation;
import reka.api.run.RouteCollector;
import reka.api.run.RoutingOperation;

public class FlowTestHelpers {

	private static final ExecutorService executor = Executors.newCachedThreadPool();

	public static FlowOperation router(final String... routeTo) {
		return new RoutingOperation() {
			
			@Override
			public void call(MutableData data, RouteCollector router) {
				for (String route : routeTo) {
					router.routeTo(route);
				}
			}
			
		};
	}

	public static FlowOperation incrementFunction(final AtomicInteger counter) {
		return new Operation() {

			@Override
			public void call(MutableData data) {
				counter.incrementAndGet();
			}
			
		};
	}

	public static FlowOperation asyncCountingFunction(final AtomicInteger counter) {
		return AsyncOperation.create((data, ctx) -> {
			executor.submit(() -> {
				counter.incrementAndGet();
				ctx.done();
			});
		});
	}
	

	
	public static FlowOperation latchFunction(final CountDownLatch latch) {
		return new Operation() {

			@Override
			public void call(MutableData data) {
				latch.countDown();
			}
			
		};
	}
}
