package reka.test;

import static java.util.Arrays.asList;
import static reka.configurer.Configurer.configure;

import java.io.File;

import org.junit.Test;

import reka.ApplicationConfigurer;
import reka.builtins.BuiltinsBundle;
import reka.config.NavigableConfig;
import reka.config.parser.ConfigParser;
import reka.config.processor.CommentConverter;
import reka.config.processor.IncludeConverter;
import reka.config.processor.MultiConverter;
import reka.config.processor.Processor;
import reka.core.bundle.BundleManager;
import reka.h2.H2Bundle;
import reka.http.HttpBundle;

public class ApplicationConfigTest {

    @Test
    public void test() {
    	BundleManager bundles = new BundleManager(asList(new BuiltinsBundle(), new HttpBundle(), new H2Bundle()));
        NavigableConfig config = ConfigParser.fromFile(new File(getClass().getResource("/app.reka").getFile()));
        config = new Processor(new MultiConverter(new CommentConverter(), new IncludeConverter())).process(config);
        configure(new ApplicationConfigurer(bundles), config).build("app", 1);
    }
    
}
