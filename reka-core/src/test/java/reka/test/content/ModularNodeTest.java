package reka.test.content;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static reka.data.content.Contents.utf8;
import static reka.runtime.handlers.DSL.actionHandlers;
import static reka.runtime.handlers.DSL.subscribers;
import static reka.util.Path.dots;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.data.Data;
import reka.data.memory.MutableMemoryData;
import reka.flow.Flow.FlowStats;
import reka.flow.ops.AsyncOperation;
import reka.flow.ops.Operation;
import reka.flow.ops.RouteKey;
import reka.flow.ops.Subscriber;
import reka.identity.IdentityStore;
import reka.runtime.DefaultFlowContext;
import reka.runtime.Node;
import reka.runtime.NodeChild;
import reka.runtime.handlers.ActionHandler;
import reka.runtime.handlers.AsyncOperationAction;
import reka.runtime.handlers.DSL;
import reka.runtime.handlers.DoNothing;
import reka.runtime.handlers.ErrorHandler;
import reka.runtime.handlers.OperationAction;
import reka.runtime.handlers.RuntimeNode;


public class ModularNodeTest {
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	@Test
	public void test() throws InterruptedException {

		final CountDownLatch latch = new CountDownLatch(1);
		
		final FlowStats stats = new FlowStats();
		
		final AtomicReference<Data> result = new AtomicReference<>();
		
		Node child = new RuntimeNode(0, "child", 
			syncOperation((data, ctx) -> data.put(dots("example.from.child"), utf8("hello from child")), 
					  subscribers((data) -> {
						  log.debug("it was called! with : {}\n", data.toPrettyJson());
						  result.set(data);
						  latch.countDown();
					  }), DoNothing.INSTANCE), DoNothing.INSTANCE, DoNothing.INSTANCE);
		
		RouteKey somechild = RouteKey.named("some child");
		
		Node parent = new RuntimeNode(1, "parent", syncOperation((data, ctx) ->
					data.putString(dots("example.from.parent"), "hello from parent"),
					actionHandlers(asList(new NodeChild(child, false, somechild).node()), DoNothing.INSTANCE), DoNothing.INSTANCE), DoNothing.INSTANCE, DoNothing.INSTANCE);
		
		parent.call(MutableMemoryData.create(), 
					DefaultFlowContext.create(1, Executors.newCachedThreadPool(), Executors.newSingleThreadExecutor(), Subscriber.DO_NOTHING, IdentityStore.emptyReader(), stats));

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
		
		final ExecutorService coordinator = Executors.newSingleThreadExecutor();
		final ExecutorService executor = Executors.newCachedThreadPool();
		final ExecutorService executor2 =Executors.newCachedThreadPool();
		final AtomicReference<Data> result = new AtomicReference<>();
		
		Node child = new RuntimeNode(0, "child", new AsyncOperationAction(AsyncOperation.create((data, ctx, res) -> { 
			executor2.submit(() -> { 
				data.put(dots("example.from.child.async"), utf8("I am from async"));
				res.done();
			});
		}), DSL.subscriber((data) -> {
			log.debug("it was called! with : {}\n", data.toPrettyJson());
			result.set(data);
			latch.countDown();
		}), DoNothing.INSTANCE), DoNothing.INSTANCE, DoNothing.INSTANCE);
		
		RouteKey somechild = RouteKey.named("some child");
		
		Node parent = new RuntimeNode(1, "parent", syncOperation((data, ctx) ->
					data.put(dots("example.from.parent"), utf8("hello from parent")),
					actionHandlers(asList(new NodeChild(child, false, somechild).node()), DoNothing.INSTANCE), DoNothing.INSTANCE), DoNothing.INSTANCE, DoNothing.INSTANCE);
		
		parent.call(MutableMemoryData.create(), DefaultFlowContext.create(1, executor, coordinator, Subscriber.DO_NOTHING, IdentityStore.emptyReader(), stats));

		

		if (latch.await(1, TimeUnit.SECONDS)) {
			assertThat(result.get().getString(dots("example.from.child.async")).orElse(""), equalTo("I am from async"));
			assertThat(result.get().getString(dots("example.from.parent")).orElse(""), equalTo("hello from parent"));
		} else {
			fail("timed out");
		}
	}
	
	public static OperationAction syncOperation(Operation operation, ActionHandler next, ErrorHandler error) {
		return new OperationAction(operation, next, error);
	}

}
