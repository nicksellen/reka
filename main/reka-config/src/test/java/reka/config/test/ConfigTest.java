package reka.config.test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.config.Config;
import reka.config.NavigableConfig;
import reka.config.parser.ConfigParser;
import reka.config.processor.IncludeConverter;
import reka.config.processor.Processor;

import com.google.common.base.Charsets;

public class ConfigTest {
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	@Test
	public void test() throws IOException {
        String filename = getClass().getResource("/config-test.conf").getFile();
		String rootSourceContent = new String(Files.readAllBytes(new File(filename).toPath()));
		NavigableConfig root = ConfigParser.fromFile(new File(filename));
		
		Processor processor = new Processor(new IncludeConverter());

		assertThat(root.elementCount(), equalTo(5));
		
		assertThat(rootSourceContent, equalTo(root.source().content()));
		
		Optional<Config> keyword1 = root.at("keyword1");
		assertTrue(keyword1.isPresent());
		assertFalse(keyword1.get().hasValue());
		assertThat(keyword1.get().key(), equalTo("keyword1"));
		assertThat(keyword1.get().source().content(), equalTo("keyword1"));
		

		assertThat(rootSourceContent, equalTo(keyword1.get().source().origin().content()));
		
		Config keyword2 = root.at("keyword2").get();
		
		assertThat(keyword2.valueAsString(), equalTo("and value"));
		assertThat(keyword2.source().content(), equalTo("keyword2 and value"));
		
		Config keyword3 = root.at("keyword3").get();
		
		assertFalse(keyword3.hasValue());
		assertTrue(keyword3.hasDocument());
		assertThat(new String(keyword3.documentContent(), Charsets.UTF_8), equalTo("and heredoc\nwith multiple lines"));
		
		Config nested = root.at("nested").get();
		assertTrue(nested.hasBody());
		assertThat(nested.body().elementCount(), equalTo(1));
		Config nestedBlock = nested.body().at("block").get();
		assertThat(nestedBlock.key(), equalTo("block"));
		
		System.out.print(root);
		
		Config imp = root.at("thing").get();
		assertThat(imp.valueAsString(), equalTo("with a @include(config-test/import.conf)"));
        
        root = processor.process(root);
        
        log.debug("--- after procssing ---\n\n{}\n\n--- ---\n", root);
		
        String rootFileContents = readFileContents(filename);
		String importedFileContents = readFileContents(filename, "config-test/import.conf");
		
		Config singleImported = root.at("thing.which").get();
		
		assertNotNull("import didn't seem to work", singleImported);
		assertThat(singleImported.source().content(), equalTo("which can import @include(aswell.conf)"));
		assertThat(singleImported.source().origin().content(), equalTo(importedFileContents));
		
		Config doubleImported = root.at("thing.which.two").get();
		log.debug(">> [{}]\n", doubleImported);
		assertThat(doubleImported.valueAsString(), equalTo("levels of import"));
		assertThat(doubleImported.source().content(), equalTo("two levels of import"));
        assertThat(doubleImported.source().rootOrigin().content(), equalTo(rootFileContents));
        assertThat(doubleImported.source().rootOrigin().location(), equalTo(filename));
		
		log.debug("dimport--START----\n{}\n----END----\n", singleImported.source());
		
	}
	
	private static String readFileContents(String base, String filename) {
	    return readFileContents(new File(base).toPath().getParent().resolve(filename).toString());
	}
	
	private static String readFileContents(String filename) {
        try {
            return new String(Files.readAllBytes(new File(filename).toPath()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
	
	
}