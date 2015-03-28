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
	private Path into = root();
	
	private boolean firstOnly = false;
    
    private StringWithVars queryFn;
	
	public JdbcQueryConfigurer(JdbcConfiguration config,boolean firstOnly) {
		this.config = config;
		this.firstOnly = firstOnly;
	}
	
	@Conf.Config
	public void config(Config config) {
	    if (config.hasDocument()) {
	        queryFn = StringWithVars.compile(config.documentContentAsString());
	        if (config.hasValue() && into == null) {
	        	into = dots(config.valueAsString());
	        }
	    } else if (config.hasValue()) {
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
    @Conf.At("into")
    public void out(String val) {
        into = dots(val);
    }
	
	@Override
	public void setup(OperationSetup ops) {
	    requireNonNull(queryFn, "you didn't pick a query!");
		ops.add("run", () -> new JdbcQuery(config, ops.ctx().get(POOL), queryFn, firstOnly, into));
	}

}
