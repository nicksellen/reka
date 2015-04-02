package reka.test.config;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.junit.Assert.assertThat;
import static reka.config.configurer.Configurer.configure;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

import reka.config.NavigableConfig;
import reka.config.parser.ConfigParser;

public class ApplyConfigTest {

	@Test
	public void test() throws IOException {

		NavigableConfig config = ConfigParser.fromFile(new File(getClass().getResource("/apply-config-test.conf").getFile()));
		
		TestConfigure output = configure(new TestConfigure(), config);

        assertThat(output.innerNames(), hasItems("name1", "name2"));
		assertThat(output.routeNames(), hasItems("inside", "get"));
        assertThat(output.blahValues(), hasItems("blah1", "blah2"));
		
	}
	
}
