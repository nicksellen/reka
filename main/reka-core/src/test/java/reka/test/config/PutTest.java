package reka.test.config;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.*;
import static reka.api.Path.dots;
import static reka.config.ConfigTestUtil.loadconfig;
import static reka.config.configurer.Configurer.configure;
import static reka.util.Util.runtime;
import static reka.util.Util.unchecked;

import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

import org.junit.Test;

import reka.api.Path.PathElement;
import reka.api.data.Data;
import reka.api.data.MutableData;
import reka.api.flow.FlowNode;
import reka.api.flow.FlowOperation;
import reka.api.flow.FlowSegment;
import reka.api.run.AsyncOperation;
import reka.api.run.OperationSupplier;
import reka.api.run.RoutingOperation;
import reka.api.run.SyncOperation;
import reka.builtins.BuiltinsModule;
import reka.config.Config;
import reka.config.NavigableConfig;
import reka.config.configurer.Configurer.InvalidConfigurationException;
import reka.core.data.memory.MutableMemoryData;
import reka.core.runtime.DefaultRouter;

public class PutTest {
	
	private final NavigableConfig root;
	
	public PutTest() {
		root = loadconfig("/put.reka");
	}
	
	@Test
	public void simple() {
		assertThat(configurePutWith("simple.put").at("name").content().asUTF8(), equalTo("nick"));
	}
	
	@Test
	public void subkey() {
		assertThat(configurePutWith("subkey.put").at("name").content().asUTF8(), equalTo("nick"));
	}
	
	@Test
	public void nested() {
		assertThat(configurePutWith("nested.put").at(dots("name.first")).content().asUTF8(), equalTo("nick"));
	}
	
	@Test(expected=InvalidConfigurationException.class)
	public void broken() {
		configurePutWith("invalid.put");
	}
	
	@Test
	public void withValAndBody() {
		Data data = configurePutWith("with-val-and-body.put");
		assertThat(data.at(dots("yay.name")).content().asUTF8(), equalTo("peter"));
		assertThat(data.at(dots("yay.age")).content().asUTF8(), equalTo("20"));
		assertThat(data.at(dots("yay.inner.@")).content().asUTF8(), equalTo("wooo"));
		assertThat(data.at(dots("yay.inner.it")).content().asUTF8(), equalTo("should"));
		assertThat(data.at(dots("yay.inner.work")).content().asUTF8(), equalTo("ok"));
	}
	
	@Test
	public void arrayIntent() {
		Data data = configurePutWith("array-intent.put");
		System.out.println(data.toPrettyJson());
		assertTrue(data.at("people").isList());
		Set<PathElement> es = data.at("people").elements();
		assertThat(es.size(), equalTo(4));
		assertThat(data.at(dots("people[0]")).content().asUTF8(), equalTo("james"));
		assertThat(data.at(dots("people[1]")).content().asUTF8(), equalTo("andrew"));
		assertThat(data.at(dots("people[2]")).content().asUTF8(), equalTo("woah"));
		assertThat(data.at(dots("people[3]")).content().asUTF8(), equalTo("nick"));
	}
	
	private Data configurePutWith(String path) {
		return configureThenCall(new BuiltinsModule.PutConfigurer(), root.at(path).get(), MutableMemoryData.create());
	}
	
	private static Data configureThenCall(Supplier<FlowSegment> s, Config config, MutableData input) {
		
		configure(s, config);
		
		OperationSupplier<?> supplier = firstNode(s.get()).operationSupplier();
		FlowOperation op = (FlowOperation) OperationSupplier.supply(supplier, Data.NONE).get();
		if (op instanceof SyncOperation) {
			return callSync((SyncOperation) op, input);
		} else if (op instanceof AsyncOperation) {
			return callAsync((AsyncOperation) op, input);
		} else if (op instanceof RoutingOperation) {
			return callRouting((RoutingOperation) op, input);
		}
		throw runtime("couldn't work out %s", op);
	}
	
	private static FlowNode firstNode(FlowSegment segment) {
		if (segment.isNode()) {
			return segment.node();
		} else {
			for (FlowSegment s : segment.segments()) {
				if (s.equals(segment)) continue;
				FlowNode n = firstNode(s);
				if (n != null) {
					return n;
				}
			}
		}
		return null;
	}

	private static Data callSync(SyncOperation op, MutableData input) {
		return op.call(input);
	}
	
	private static Data callAsync(AsyncOperation op, MutableData input) {
		try {
			return op.call(input).get();
		} catch (InterruptedException | ExecutionException e) {
			throw unchecked(e);
		}
	}
	
	private static Data callRouting(RoutingOperation op, MutableData input) {
		return op.call(input, DefaultRouter.create(new ArrayList<String>()));
	}

}
