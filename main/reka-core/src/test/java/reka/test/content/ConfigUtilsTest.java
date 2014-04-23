package reka.test.content;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.api.data.Data;
import reka.config.Config;
import reka.config.NavigableConfig;
import reka.config.parser.ConfigParser;
import reka.core.config.ConfigUtils;

public class ConfigUtilsTest {
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	@Test
	public void test() {
		NavigableConfig config = ConfigParser.fromString("name nick\nname peter\ncool\n!awesome\njohn {\n  age 29\n}");
		Config c = config.at("john").get();
		Data data = ConfigUtils.configToData(c.body());
		
		log.debug("turned [{}] into [{}]", config, data.toPrettyJson());
	}

}
