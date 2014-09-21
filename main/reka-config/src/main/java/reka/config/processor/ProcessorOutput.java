package reka.config.processor;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import reka.config.Config;
import reka.config.ConfigBody;
import reka.config.Source;
import reka.config.parser.values.KeyAndSubkey;
import reka.config.processor.ConfigConverter.Output;

class ProcessorOutput implements ConfigConverter.Output {

	private final Output toplevel;
	private final List<Config> configs;
	private final Source source;
	private final int depth;
    
	public ProcessorOutput(Output toplevel, Source source, int depth) {
	    this(toplevel, source, null, depth);
	}
	
	private ProcessorOutput(Output toplevel, Source source, List<Config> configs, int depth) {
		this.toplevel = toplevel != null ? toplevel : this;
	    this.source = source;
	    this.configs = configs != null ? configs : new ArrayList<Config>();
	    this.depth = depth;
	}
	
	private int mark;
	
	@Override
	public void mark() {
	    mark = configs.size();
	}
	
	public int size() {
		return configs.size();
	}
	
	List<Config> changed() {
	    if (mark < configs.size() - 1) {
	        return Collections.emptyList();
	    } else {
	        return configs.subList(mark, configs.size());
	    }
	}
	
	public Iterable<Config> configs() {
	    return configs;
	}
	
	@Override
	public Output add(Config config) {
	    checkArgument(config.source() != null, "cannot add a config without a source [%s]", config);
		configs.add(config);
		return this;
	}

    @Override
    public Output add(Iterable<Config> configs) {
        for (Config c : configs) {
            add(c);
        }
        return this;
    }

	@Override
	public Output key(KeyAndSubkey key) {
		return add(Config.newBuilder().source(source).keyAndSubkey(key).build());
	}

	@Override
	public Output keyvalue(KeyAndSubkey key, String value) {
		return add(Config.newBuilder()
        		.source(source)
        		.keyAndSubkey(key)
        		.value(value).build());
	}

	@Override
	public Output doc(KeyAndSubkey key, String type, byte[] content) {
		return add(Config.newBuilder()
    			.source(source)
    			.keyAndSubkey(key)
    			.document(type, content).build());
	}

	@Override
	public Output doc(KeyAndSubkey key, Object value, String type, byte[] content) {
		return add(Config.newBuilder()
    			.source(source)
    			.keyAndSubkey(key)
    			.value(value)
    			.document(type, content).build());
	}

    @Override
    public Output obj(KeyAndSubkey key, Iterable<Config> children) {
    	return obj(key, null, children);
    }
    
    @Override
    public Output obj(KeyAndSubkey key, Object value, Iterable<Config> children) {
        return add(Config.newBuilder()
        		.source(source)
        		.keyAndSubkey(key)
        		.value(value)
        		.body(ConfigBody.of(source, children)).build());
    }

	@Override
	public Output embed(Config config) {
		if (config.hasBody()) {
			for (Config child : config.body()) {
				add(child);
			}
		}
		return this;
	}

    @Override
    public Output reset() {
        configs.clear();
        return this;
    }

    @Override
    public Output obj(KeyAndSubkey key, Config... children) {
        return obj(key, ConfigBody.of(source, children));
    }

    @Override
    public Output obj(KeyAndSubkey key, Object value, Config... children) {
        return obj(key, value, ConfigBody.of(source, children));
    }
    
	@Override
	public Output toplevel() {
		return toplevel;
	}
	
	@Override 
	public boolean isTopLevel() {
		return this == toplevel;
	}
	
	@Override
	public int depth() {
		return depth;
	}
}