package reka.test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import reka.data.MutableData;
import reka.flow.FlowOperation;
import reka.flow.ops.AsyncOperation;
import reka.flow.ops.Operation;
import reka.flow.ops.OperationContext;
import reka.flow.ops.RouteCollector;
import reka.flow.ops.RouteKey;
import reka.flow.ops.RouterOperation;

public class FlowTestHelpers {

	private static final ExecutorService executor = Executors.newCachedThreadPool();

	public static FlowOperation router(final RouteKey... routeTo) {
		return new RouterOperation() {
			
			@Override
			public void call(MutableData data, RouteCollector router) {
				for (RouteKey key : routeTo) {
					router.routeTo(key);
				}
			}
			
		};
	}

	public static FlowOperation incrementFunction(final AtomicInteger counter) {
		return new Operation() {

			@Override
			public void call(MutableData data, OperationContext ctx) {
				counter.incrementAndGet();
			}
			
		};
	}

	public static FlowOperation asyncCountingFunction(final AtomicInteger counter) {
		return AsyncOperation.create((data, ctx, res) -> {
			executor.submit(() -> {
				counter.incrementAndGet();
				res.done();
			});
		});
	}
	

	
	public static FlowOperation latchFunction(final CountDownLatch latch) {
		return new Operation() {

			@Override
			public void call(MutableData data, OperationContext ctx) {
				latch.countDown();
			}
			
		};
	}
}
