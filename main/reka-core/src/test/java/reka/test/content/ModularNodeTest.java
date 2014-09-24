package reka.test.content;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static reka.api.Path.dots;
import static reka.api.content.Contents.utf8;
import static reka.core.runtime.handlers.DSL.actionHandlers;
import static reka.core.runtime.handlers.DSL.subscribers;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.api.data.Data;
import reka.api.flow.Flow.FlowStats;
import reka.api.run.AsyncOperation;
import reka.api.run.Operation;
import reka.api.run.RouteKey;
import reka.api.run.Subscriber;
import reka.core.data.memory.MutableMemoryData;
import reka.core.runtime.DefaultFlowContext;
import reka.core.runtime.Node;
import reka.core.runtime.NodeChild;
import reka.core.runtime.handlers.ActionHandler;
import reka.core.runtime.handlers.AsyncOperationAction;
import reka.core.runtime.handlers.DSL;
import reka.core.runtime.handlers.DoNothing;
import reka.core.runtime.handlers.OperationAction;
import reka.core.runtime.handlers.RuntimeNode;


public class ModularNodeTest {
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	@Test
	public void test() throws InterruptedException {

		final CountDownLatch latch = new CountDownLatch(1);
		
		final FlowStats stats = new FlowStats();
		
		final AtomicReference<Data> result = new AtomicReference<>();
		
		Node child = new RuntimeNode(0, "child", 
			syncOperation((data) -> data.put(dots("example.from.child"), utf8("hello from child")), 
					  subscribers((data) -> {
						  log.debug("it was called! with : {}\n", data.toPrettyJson());
						  result.set(data);
						  latch.countDown();
					  })), DoNothing.INSTANCE, DoNothing.INSTANCE);
		
		RouteKey somechild = RouteKey.named("some child");
		
		Node parent = new RuntimeNode(1, "parent", syncOperation((data) ->
					data.putString(dots("example.from.parent"), "hello from parent"),
					actionHandlers(asList(new NodeChild(child, false, somechild).node()), DoNothing.INSTANCE)), DoNothing.INSTANCE, DoNothing.INSTANCE);
		
		parent.call(MutableMemoryData.create(), 
					DefaultFlowContext.create(1, Executors.newCachedThreadPool(), Subscriber.DO_NOTHING, stats));

		if (latch.await(1, TimeUnit.SECONDS)) {
			assertThat(result.get().getString(dots("example.from.child")).orElse("not found"), equalTo("hello from child"));
			assertThat(result.get().getString(dots("example.from.parent")).orElse("not found"), equalTo("hello from parent"));
		} else {
			fail("timed out");
		}
	}
	
	@Test
	public void testAsync() throws InterruptedException {
		
		final CountDownLatch latch = new CountDownLatch(1);
		
		final FlowStats stats = new FlowStats();
		
		final ExecutorService executor = Executors.newCachedThreadPool();
		final ExecutorService executor2 =Executors.newCachedThreadPool();
		final AtomicReference<Data> result = new AtomicReference<>();
		
		Node child = new RuntimeNode(0, "child", new AsyncOperationAction(AsyncOperation.create((data, ctx) -> { 
			executor2.submit(() -> { 
				data.put(dots("example.from.child.async"), utf8("I am from async"));
				ctx.done();
			});
		}), DSL.subscriber((data) -> {
			log.debug("it was called! with : {}\n", data.toPrettyJson());
			result.set(data);
			latch.countDown();
		}), DoNothing.INSTANCE), DoNothing.INSTANCE, DoNothing.INSTANCE);
		
		RouteKey somechild = RouteKey.named("some child");
		
		Node parent = new RuntimeNode(1, "parent", syncOperation((data) ->
					data.put(dots("example.from.parent"), utf8("hello from parent")),
					actionHandlers(asList(new NodeChild(child, false, somechild).node()), DoNothing.INSTANCE)), DoNothing.INSTANCE, DoNothing.INSTANCE);
		
		parent.call(MutableMemoryData.create(), DefaultFlowContext.create(1, executor, Subscriber.DO_NOTHING, stats));

		

		if (latch.await(1, TimeUnit.SECONDS)) {
			assertThat(result.get().getString(dots("example.from.child.async")).orElse(""), equalTo("I am from async"));
			assertThat(result.get().getString(dots("example.from.parent")).orElse(""), equalTo("hello from parent"));
		} else {
			fail("timed out");
		}
	}
	
	public static OperationAction syncOperation(Operation operation, ActionHandler next) {
		return new OperationAction(operation, next);
	}

}
