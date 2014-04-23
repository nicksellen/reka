package reka.config.processor;

import java.util.ArrayList;
import java.util.List;

import reka.config.Config;
import reka.config.ConfigBody;
import reka.config.NavigableConfig;
import reka.config.processor.ConfigConverter.Output;

import com.google.common.collect.Iterables;

public class Processor {

    private static final int MAX_ITERATIONS = 5;
    
    private final ConfigConverter converter;
    
    public Processor(ConfigConverter converter) {
        this.converter = converter;
    }
	
	public NavigableConfig process(NavigableConfig original) {
		ProcessorOutput toplevel = new ProcessorOutput(null, original.source());
	    NavigableConfig from = null, to = original;
	    for (int i = 0; from != to && i < MAX_ITERATIONS; i++) {
            from = to;
            to = rebuild(toplevel, from, 0);
	    }
	    return to;
	}
    
	private void processChildren(ProcessorOutput toplevel, Config config, ProcessorOutput out, int depth) {
	    ProcessorOutput out2 = new ProcessorOutput(toplevel, config.source());
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
            out.obj(config.key(), config.value(), out2.configs());
        } else {
            out.add(config);
        }
	}
	
	private ConfigBody convert(Output toplevel, ConfigBody body) {
	    ProcessorOutput out = new ProcessorOutput(toplevel, new ConvertedSource(converter, body.source()));
	    for (Config config : body) {
	        out.mark();
            converter.convert(config, out);
        }
	    return ConfigBody.of(body.source(), out.configs());
	}
	
    private NavigableConfig rebuild(ProcessorOutput toplevel, NavigableConfig input, int depth) {

        ProcessorOutput out = new ProcessorOutput(toplevel, input.source());
    	for (Config config : input.each()) {
            if (config.hasBody()) {
               processChildren(toplevel, config, out, depth);
            } else {
                out.add(config);
            } 
    	}

    	toplevel.mark();
    	NavigableConfig result = convert(toplevel, ConfigBody.of(input.source(), out.configs()));
    	
    	if (depth == 0) {
        	List<Config> finaloutput = new ArrayList<>();
        	Iterables.addAll(finaloutput, result);
        	Iterables.addAll(finaloutput, toplevel.changed());
		    result = ConfigBody.of(result.source(), finaloutput);
    	}
    	
    	return input.equals(result) ? input : result;
    }   
	
}
