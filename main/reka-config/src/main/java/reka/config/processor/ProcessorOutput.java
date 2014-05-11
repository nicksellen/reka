package reka.config.processor;

import static com.google.common.base.Preconditions.checkArgument;
import static reka.config.ConfigUtil.k;
import static reka.config.ConfigUtil.kv;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import reka.config.Config;
import reka.config.ConfigBody;
import reka.config.ConfigUtil;
import reka.config.Source;
import reka.config.processor.ConfigConverter.Output;

class ProcessorOutput implements ConfigConverter.Output {

	private final Output toplevel;
	private final List<Config> configs;
	private final Source source;
	private final String[] path;
    
	public ProcessorOutput(Output toplevel, Source source, String[] path) {
	    this(toplevel, source, null, path);
	}
	
	private ProcessorOutput(Output toplevel, Source source, List<Config> configs, String[] path) {
		this.toplevel = toplevel != null ? toplevel : this;
	    this.source = source;
	    this.configs = configs != null ? configs : new ArrayList<Config>();
	    this.path = path;
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
	public Output key(String keyword) {
		return add(k(source, keyword));
	}

	@Override
	public Output keyvalue(String key, String value) {
		return add(kv(source, key, value));
	}

	@Override
	public Output doc(String key, String type, byte[] content) {
		return add(ConfigUtil.doc(source, key, type, content));
	}

	@Override
	public Output doc(String key, Object value, String type, byte[] content) {
		return add(ConfigUtil.doc(source, key, value, type, content));
	}

    @Override
    public Output obj(String key, Iterable<Config> children) {
    	return obj(key, null, children);
    }
    
    @Override
    public Output obj(String key, Object value, Iterable<Config> children) {
        return add(ConfigUtil.obj(source, key, value, children));
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
    public Output obj(String key, Config... children) {
        return obj(key, ConfigBody.of(source, children));
    }

    @Override
    public Output obj(String key, Object value, Config... children) {
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
	public String[] path() {
		return path;
	}
}