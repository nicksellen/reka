package nicksellen.config;

import static com.google.common.collect.Iterables.toArray;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static reka.config.ConfigTestUtil.loadconfig;

import org.junit.Test;

import reka.config.Config;
import reka.config.NavigableConfig;

import com.google.common.base.Splitter;

public class ConfigDocumentTest {
	
	@Test
	public void test() {
		NavigableConfig root = loadconfig("/doc.reka");
		Config t = root.at("text-document").orNull();
		assertNotNull(t);
		assertTrue(t.hasDocument());
		assertThat(t.documentType(), equalTo("text/plain"));
		assertThat(t.documentContentAsString(), equalTo("this is my text document"));
	}
	
	@Test
	public void test2() {
		NavigableConfig root = loadconfig("/doc.reka");
		Config t = root.at("text-document-with-dashes").orNull();
		assertNotNull(t);
		assertTrue(t.hasDocument());
		assertThat(t.documentType(), equalTo("text/plain"));
		String[] lines = toArray(Splitter.on("\n").split(t.documentContentAsString()), String.class);
		assertThat(lines[2], equalTo("- some nice"));
		assertThat(lines.length, equalTo(11));
	}

}
