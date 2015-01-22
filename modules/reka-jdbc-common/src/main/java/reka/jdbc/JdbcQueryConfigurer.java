package reka.jdbc;

import static java.util.Objects.requireNonNull;
import static reka.api.Path.dots;
import static reka.api.Path.root;
import static reka.jdbc.JdbcBaseModule.POOL;
import reka.api.Path;
import reka.config.Config;
import reka.config.configurer.annotations.Conf;
import reka.core.setup.OperationConfigurer;
import reka.core.setup.OperationSetup;
import reka.core.util.StringWithVars;

public class JdbcQueryConfigurer implements OperationConfigurer {

	private final JdbcConfiguration config;
	private Path out = root();
	
	private boolean first = false;
    
    private StringWithVars queryFn;
	
	public JdbcQueryConfigurer(JdbcConfiguration config,boolean first) {
		this.config = config;
		this.first = first;
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
	public void setup(OperationSetup ops) {
	    requireNonNull(queryFn, "you didn't pick a query!");
		ops.add("run", store -> new JdbcQuery(config, store.get(POOL), queryFn, first, out));
	}

}
