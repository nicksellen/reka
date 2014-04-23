package reka.test.config;

import static reka.configurer.Configurer.configure;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.config.Config;
import reka.configurer.annotations.Conf;

public class TestConfigure {
	
	private static final Logger log = LoggerFactory.getLogger(TestConfigure.class);
    
    private final List<Inner> inners = new ArrayList<>();
    private final List<String> blahs = new ArrayList<>();
    private Router router;
    
	@Conf.At("people.nick.age")
	public void node(BigDecimal age) {
		log.debug("nick is {}\n", age);
	}

	@Conf.At("people.james.age")
	public void james(BigDecimal age) {
		log.debug("james is {}\n", age);
	}
	
	@Conf.At("description")
	public void node(String description) {
		log.debug("got description [{}]\n", description);
	}
	
	@Conf.Each("inner")
	public void each(Config config) {
	    inners.add(configure(new Inner(), config));
	}
	
	@Conf.At("router")
	public void router(Config config) {
	    log.debug("adding router [{}]\n", config);
	    router = configure(new Router(), config);
	}
	
	@Conf.Each("flows.blah")
	public void blah(Config config) {
	    blahs.add(config.valueAsString());
	    log.debug("flows.blah -> {}\n", config.valueAsString());
	}
	
	public List<String> routeNames() {
	    return router.gets;
	}
	
	public List<String> blahValues() {
	    return blahs;
	}
	
	public List<String> innerNames() {
	    List<String> names = new ArrayList<>();
	    for (Inner inner : inners) {
	        names.add(inner.name);
	    }
	    return names;
	}
	
	public static class Inner {
	    
	    private String name;
	    
	    @Conf.At("name")
	    public void name(String name) {
	        log.debug("inner got name [{}]\n", name);
	        this.name = name;
	    }
	    
	}
	
	public static class Router {
	    
	    private final List<String> gets = new ArrayList<>();
	    
	    @Conf.Each("GET")
	    public void get(Config config) {
	        for (Config child : config.body()) {
	            gets.add(child.key());
	        }
	    }
	    
	}
	
}
