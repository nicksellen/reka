package reka.config.test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

import reka.config.NavigableConfig;
import reka.config.parser.ConfigParser;

public class ConfigFormattingTest {
	
	@Test
    public void blah() throws IOException {

    	NavigableConfig root = ConfigParser.fromFile(new File(getClass().getResource("/simple.conf").getFile()));

    	String original = root.toString();
    	
		NavigableConfig current = root;
    	// see if it can read itself!
    	for (int i = 0; i < 100; i++) {
    		String formatted = current.toString();
    		assertThat(formatted, equalTo(original));
    		current = ConfigParser.fromString(formatted);
    	}
    }
    
    
    
}
