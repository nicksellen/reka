package reka.jdbc;

import static com.google.common.base.Preconditions.checkNotNull;
import static reka.api.Path.dots;
import static reka.api.Path.root;
import static reka.jdbc.JdbcModule.POOL;
import reka.api.Path;
import reka.config.Config;
import reka.config.configurer.annotations.Conf;
import reka.core.bundle.OperationSetup;
import reka.core.util.StringWithVars;
import reka.nashorn.OperationsConfigurer;

public class JdbcQueryConfigurer implements OperationsConfigurer {

	private final JdbcConfiguration config;
	private Path out = root();
    
    private StringWithVars queryFn;
	
	public JdbcQueryConfigurer(JdbcConfiguration config) {
		this.config = config;
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
	    checkNotNull(queryFn, "you didn't pick a query!");
		ops.add("jdbc/query", store -> new JdbcQuery(config, store.get(POOL), queryFn, out));
	}

}
