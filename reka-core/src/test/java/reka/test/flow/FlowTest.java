package reka.test.flow;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static reka.flow.builder.FlowSegments.parallel;
import static reka.flow.builder.OperationFlowNode.asyncOperation;
import static reka.flow.builder.OperationFlowNode.operation;
import static reka.util.Path.dots;
import static reka.util.Path.path;
import static reka.util.Util.unchecked;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Test;

import reka.data.Data;
import reka.data.DiffContentConsumer;
import reka.data.MutableData;
import reka.data.content.Content;
import reka.data.memory.MutableMemoryData;
import reka.flow.Flow;
import reka.flow.builder.FlowBuilderGroup;
import reka.util.Path;

public class FlowTest {
	
	private static final ExecutorService otherExecutor = Executors.newCachedThreadPool();
	
	private MutableData mutateData1(MutableData data) {
		data.putString("name", "alex");
		data.putMap("nick", map -> {
			map.putString("name", "nick");
			map.putInt("age", 31);
			map.putList("interests", interests -> {
				interests.addString("cycling");
				interests.addString("running");
			});
		});
		return data;
	}
	
	private MutableData mutateData2(MutableData data) {
		data.putString("boo", "yeah");
		return data;
	}

	private MutableData mutateData3(MutableData data) {
		data.putString(dots("nick.location"), "london");
		return data;
	}

	private MutableData mutateData4(MutableData data) {
		data.putString(dots("bom.de.bom"), "yay");
		return data;
	}
	
	@Test
	public void test() {
		
		Data refData = mutateData4(mutateData3(mutateData2(mutateData1(MutableMemoryData.create())))).immutable();
		
		AtomicLong counter = new AtomicLong();
		
		Flow flow = FlowBuilderGroup.createFlow(path("flow"), 
			parallel(
				operation("mutate1", (data, ctx) -> {
					mutateData1(data);
					counter.incrementAndGet();
				}),
				asyncOperation("mutate2", (data, ctx, res) -> {
					otherExecutor.execute(() -> {
						mutateData2(data);
						res.done();
						counter.incrementAndGet();
					});
				}),
				operation("mutate3", (data, ctx) -> {
					mutateData3(data);
					counter.incrementAndGet();
				}),
				asyncOperation("mutate4", (data, ctx, res) -> {
					otherExecutor.execute(() -> {
						mutateData4(data);
						res.done();
						counter.incrementAndGet();
					});
				})
			)
		);
		
		ExecutorService e1 = Executors.newCachedThreadPool();
		ExecutorService e2 = Executors.newSingleThreadExecutor();
		
		int n = 1000;
		CountDownLatch latch = new CountDownLatch(n);
		
		for (int i = 0; i < n; i++) {
			flow.prepare().operationExecutor(e1).coordinationExecutor(e2).complete(data -> {
				try {
					assertThat(changeCount(refData, data), equalTo(0));
				} finally {
					latch.countDown();
				}
			}).run();
		}
			
		try {
			if (latch.await(1, TimeUnit.SECONDS)) {
				assertThat(counter.get(), equalTo(n * 4L));
			} else {
				fail("timed out");
			}
		} catch (InterruptedException e) {
			throw unchecked(e);
		}
	}
	
	private int changeCount(Data a, Data b) {
		final AtomicInteger changeCount = new AtomicInteger();
		a.diffContentTo(b, new DiffContentConsumer(){

			@Override
			public void accept(Path path, DiffContentType type, Content prev, Content current) {
				changeCount.incrementAndGet();
			}
			
		});
		return changeCount.get();
	}

}
