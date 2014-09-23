package reka.config.processor;

import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;

import java.util.ArrayList;
import java.util.List;

import reka.config.Config;
import reka.config.ConfigBody;
import reka.config.NavigableConfig;
import reka.config.parser.values.KeyAndSubkey;
import reka.config.processor.ConfigConverter.Output;

public class Processor {
	
    private static final int MAX_ITERATIONS = 5;
    
    private final ConfigConverter converter;
    
    public Processor(ConfigConverter converter) {
        this.converter = converter;
    }
	
	public NavigableConfig process(NavigableConfig original) {
		ProcessorOutput toplevel = new ProcessorOutput(null, original.source(), 0);
	    NavigableConfig from = null, to = original;
	    for (int i = 0; from != to && i < MAX_ITERATIONS; i++) {
            from = to;
            to = rebuild(toplevel, from, 0);
	    }
	    return to;
	}
    
	private void processChildren(ProcessorOutput toplevel, Config config, ProcessorOutput out, int depth) {
	    ProcessorOutput out2 = new ProcessorOutput(toplevel, config.source(), depth);
        boolean changed = false;
        for (Config child : config.body()) {
            NavigableConfig originalChild = ConfigBody.of(child.source(), child);
            NavigableConfig rebuiltChild = rebuild(toplevel, originalChild, depth + 1);
            if (!originalChild.equals(rebuiltChild)) {
                changed = true;
            }
            out2.add(rebuiltChild);
        }
        if (changed) {
            out.obj(new KeyAndSubkey(config), config.value(), out2.configs());
        } else {
            out.add(config);
        }
	}
	
	private ConfigBody convert(Output toplevel, ConfigBody body, int depth) {
	    ProcessorOutput out = new ProcessorOutput(toplevel, new ConvertedSource(converter, body.source()), depth);
	    for (Config config : body) {
	        out.mark();
            converter.convert(config, out);
        }
	    return ConfigBody.of(body.source(), out.configs());
	}
	
    private NavigableConfig rebuild(ProcessorOutput toplevel, NavigableConfig input, int depth) {

		input = convert(toplevel, ConfigBody.of(input.source(), input), depth);
		
        ProcessorOutput out = new ProcessorOutput(toplevel, input.source(), depth);
        
    	for (Config config : input.each()) {
    		
            if (config.hasBody()) {
               processChildren(toplevel, config, out, depth);
            } else {
                out.add(config);
            } 
    	}

    	toplevel.mark();
    	
    	NavigableConfig result = ConfigBody.of(input.source(), out.configs());
    	
    	if (depth == 0) {
        	List<Config> all = new ArrayList<>();
        	all.addAll(stream(result.spliterator(), false).collect(toList()));
        	all.addAll(stream(toplevel.changed().spliterator(), false).collect(toList()));
		    result = ConfigBody.of(result.source(), all);
    	}
    	
    	return input.equals(result) ? input : result;
    }   
	
}
