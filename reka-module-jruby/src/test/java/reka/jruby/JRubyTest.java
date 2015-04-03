package reka.jruby;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static reka.util.Path.dots;
import static reka.util.Path.path;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.Test;

import reka.data.MutableData;
import reka.data.memory.MutableMemoryData;
import reka.flow.ops.Operation;
import reka.flow.ops.OperationContext;
import reka.identity.IdentityStore;

import com.google.common.io.Resources;

public class JRubyTest {

	private final RubyEnv env = RubyEnv.create("source 'https://rubygems.org'\ngem 'nokogiri'");
	
	@Test
	public void test() {
		Operation op = new JRubyRunOperation(env, "\"name is #{data['name']}\"", path("out"));
		System.out.printf("start\n");
		for (int i = 0; i < 10000; i++) {
			MutableData data = MutableMemoryData.create();
			data.putString("name", "omg " + i);
			op.call(data, new OperationContext(IdentityStore.emptyReader()));
			assertThat(data.getString("out").get(), equalTo("name is omg " + i));
		}
		System.out.printf("end\n");
	}
	
	@Test
	public void test2() throws IOException {

		String test2initrb = Resources.toString(getClass().getResource("/test2-init.rb"), StandardCharsets.UTF_8);
		String test2rb = Resources.toString(getClass().getResource("/test2.rb"), StandardCharsets.UTF_8);
		
		env.exec(test2initrb);
		
		Operation op = new JRubyRunOperation(env, test2rb, path("out"));
		MutableData data = MutableMemoryData.create();
		data.putString(dots("something.deep.in.here"), "yay");
		data.putString("name", "omg");
		op.call(data, new OperationContext(IdentityStore.emptyReader()));
		System.out.printf("data is: %s\n", data.toPrettyJson());
	}
	
	@Test
	public void test3() throws IOException {
		String test3rb = Resources.toString(getClass().getResource("/test3.rb"), StandardCharsets.UTF_8);
		env.exec(test3rb);
	}
	
}
