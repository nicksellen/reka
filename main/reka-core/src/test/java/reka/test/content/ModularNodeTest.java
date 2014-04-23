package reka.test.content;

import static java.util.Arrays.asList;
import static junit.framework.Assert.fail;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static reka.api.Path.dots;
import static reka.api.content.Contents.utf8;
import static reka.core.runtime.handlers.DSL.actionHandlers;
import static reka.core.runtime.handlers.DSL.asyncOperation;
import static reka.core.runtime.handlers.DSL.subscriber;
import static reka.core.runtime.handlers.DSL.subscribers;
import static reka.core.runtime.handlers.DSL.syncOperation;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.api.data.Data;
import reka.core.data.memory.MutableMemoryData;
import reka.core.runtime.DefaultFlowContext;
import reka.core.runtime.Node;
import reka.core.runtime.NodeChild;
import reka.core.runtime.handlers.DoNothing;
import reka.core.runtime.handlers.RuntimeNode;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

public class ModularNodeTest {
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	@SuppressWarnings("deprecation")
	@Test
	public void test() throws InterruptedException {

		final CountDownLatch latch = new CountDownLatch(1);
		
		final AtomicReference<Data> result = new AtomicReference<>();
		
		Node child = new RuntimeNode(0, "child", 
			syncOperation((data) -> data.put(dots("example.from.child"), utf8("hello from child")), 
					  subscribers((data) -> {
						  log.debug("it was called! with : {}\n", data.toPrettyJson());
						  result.set(data);
						  latch.countDown();
					  })), DoNothing.INSTANCE, DoNothing.INSTANCE);
		
		Node parent = new RuntimeNode(1, "parent", syncOperation((data) ->
					data.put(dots("example.from.parent"), utf8("hello from parent")),
					actionHandlers(asList(new NodeChild(child, false, "some child").node()))), DoNothing.INSTANCE, DoNothing.INSTANCE);
		
		parent.call(MutableMemoryData.create(), 
					new DefaultFlowContext(1, MoreExecutors.listeningDecorator(Executors.newCachedThreadPool()), null));

		if (latch.await(1, TimeUnit.SECONDS)) {
			assertThat(result.get().getString(dots("example.from.child")).orElse("not found"), equalTo("hello from child"));
			assertThat(result.get().getString(dots("example.from.parent")).orElse("not found"), equalTo("hello from parent"));
		} else {
			fail("timed out");
		}
	}
	
	@SuppressWarnings("deprecation")
	@Test
	public void testAsync() throws InterruptedException {
		
		final CountDownLatch latch = new CountDownLatch(1);
		
		final ListeningExecutorService executor = MoreExecutors.listeningDecorator(Executors.newCachedThreadPool());
		final ListeningExecutorService executor2 = MoreExecutors.listeningDecorator(Executors.newCachedThreadPool());
		final AtomicReference<Data> result = new AtomicReference<>();
		
		Node child = new RuntimeNode(0, "child", 
			asyncOperation(
					(data) -> 
						executor2.submit(() -> { 
							return data.put(dots("example.from.child.async"), utf8("I am from async"));
						}),
					subscriber((data) -> {
						log.debug("it was called! with : {}\n", data.toPrettyJson());
						result.set(data);
						latch.countDown();
					}), DoNothing.INSTANCE), DoNothing.INSTANCE, DoNothing.INSTANCE);
		
		Node parent = new RuntimeNode(1, "parent", syncOperation((data) ->
					data.put(dots("example.from.parent"), utf8("hello from parent")),
					actionHandlers(asList(new NodeChild(child, false, "some child").node()))), DoNothing.INSTANCE, DoNothing.INSTANCE);
		
		parent.call(MutableMemoryData.create(), new DefaultFlowContext(1, executor, null));

		

		if (latch.await(1, TimeUnit.SECONDS)) {
			assertThat(result.get().getString(dots("example.from.child.async")).orElse(""), equalTo("I am from async"));
			assertThat(result.get().getString(dots("example.from.parent")).orElse(""), equalTo("hello from parent"));
		} else {
			fail("timed out");
		}
	}

}
