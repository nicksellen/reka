package nicksellen.config;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Optional;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.config.Config;
import reka.config.NavigableConfig;
import reka.config.parser.ConfigParser;

public class ConfigSourceTest {
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	@Test
	public void blah() {
		NavigableConfig root = ConfigParser.fromFile(new File(getClass().getResource("/source-test.conf").getFile()));
		
		Optional<Config> keyword1 = root.at("keyword1");
		assertTrue(keyword1.isPresent());
		assertFalse(keyword1.get().hasValue());
		assertThat(keyword1.get().key(), equalTo("keyword1"));
		
		assertThat(keyword1.get().source().content(), equalTo("keyword1"));
		
		Optional<Config> keyword2 = root.at("keyword2");
		assertTrue(keyword2.isPresent());
		assertThat(keyword2.get().valueAsString(), equalTo("and value"));
		
		log.debug("source [{}]\n", keyword2.get().source().content());
		
	}

}
