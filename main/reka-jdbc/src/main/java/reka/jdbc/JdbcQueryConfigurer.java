package reka.jdbc;

import static com.google.common.base.Preconditions.checkNotNull;
import static reka.api.Path.dots;
import static reka.api.Path.path;
import static reka.api.Path.root;
import static reka.core.builder.FlowSegments.sync;

import java.util.function.Supplier;

import reka.api.Path;
import reka.api.flow.FlowSegment;
import reka.config.Config;
import reka.configurer.annotations.Conf;
import reka.core.util.StringWithVars;

public class JdbcQueryConfigurer implements Supplier<FlowSegment> {

	private final JdbcConfiguration config;
	private final JdbcConnectionProvider provider;
	private Path out = root();
    
    private StringWithVars queryFn;
	
	public JdbcQueryConfigurer(JdbcConfiguration config, JdbcConnectionProvider provider) {
		this.config = config;
		this.provider = provider;
	}
	
	@Conf.Config
	public void config(Config config) {
	    if (config.hasDocument()) {
	        queryFn = StringWithVars.compile(config.documentContentAsString());
	        if (config.hasValue() && out == null) {
	        	out = dots(config.valueAsString());
	        }
	    } else if (config.hasValue() && !config.hasBody()) {
	        queryFn = StringWithVars.compile(config.valueAsString());
	    }
	}
	    
    @Conf.At("query")
    public void query(Config config) {
        if (config.hasDocument()) {
            queryFn = StringWithVars.compile(config.documentContentAsString());
        } else {
            queryFn = StringWithVars.compile(config.valueAsString());
        }
    }
    
    @Conf.At("out")
    public void out(String val) {
        out = dots(val);
    }
	
	@Override
	public FlowSegment get() {
	    checkNotNull(queryFn, "you didn't pick a query!");
		return sync("jdbc/query", () -> new JdbcQuery(config, provider, queryFn, out));
	}

}
