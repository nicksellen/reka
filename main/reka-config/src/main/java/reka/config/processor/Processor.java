package reka.config.processor;

import java.util.ArrayList;
import java.util.List;

import reka.config.Config;
import reka.config.ConfigBody;
import reka.config.NavigableConfig;
import reka.config.parser.values.KeyVal;
import reka.config.processor.ConfigConverter.Output;

import com.google.common.collect.Iterables;

public class Processor {
	
    private static final int MAX_ITERATIONS = 5;
    
    private final ConfigConverter converter;
    
    public Processor(ConfigConverter converter) {
        this.converter = converter;
    }
	
	public NavigableConfig process(NavigableConfig original) {
		ProcessorOutput toplevel = new ProcessorOutput(null, original.source(), new String[]{});
	    NavigableConfig from = null, to = original;
	    for (int i = 0; from != to && i < MAX_ITERATIONS; i++) {
            from = to;
            to = rebuild(toplevel, from, new String[]{});
	    }
	    return to;
	}
    
	private void processChildren(ProcessorOutput toplevel, Config config, ProcessorOutput out, String[] path) {
	    ProcessorOutput out2 = new ProcessorOutput(toplevel, config.source(), path);
        boolean changed = false;
        for (Config child : config.body()) {
            NavigableConfig originalChild = ConfigBody.of(child.source(), child);
            NavigableConfig rebuiltChild = rebuild(toplevel, originalChild, append(path, child.key()));
            if (!originalChild.equals(rebuiltChild)) {
                changed = true;
            }
            out2.add(rebuiltChild);
        }
        if (changed) {
            out.obj(new KeyVal(config), config.value(), out2.configs());
        } else {
            out.add(config);
        }
	}
	
	private static String[] append(String[] arr, String v) {
		String[] out = new String[arr.length + 1];
		out[out.length - 1] = v;
		return out;
	}
	
	private ConfigBody convert(Output toplevel, ConfigBody body, String[] path) {
	    ProcessorOutput out = new ProcessorOutput(toplevel, new ConvertedSource(converter, body.source()), path);
	    for (Config config : body) {
	        out.mark();
            converter.convert(config, out);
        }
	    return ConfigBody.of(body.source(), out.configs());
	}
	
    private NavigableConfig rebuild(ProcessorOutput toplevel, NavigableConfig input, String[] path) {

		input = convert(toplevel, ConfigBody.of(input.source(), input), path);
		
        ProcessorOutput out = new ProcessorOutput(toplevel, input.source(), path);
        
    	for (Config config : input.each()) {
    		
            if (config.hasBody()) {
               processChildren(toplevel, config, out, path);
            } else {
                out.add(config);
            } 
    	}

    	toplevel.mark();
    	
    	NavigableConfig result = ConfigBody.of(input.source(), out.configs());
    	
    	if (path.length == 0) {
        	List<Config> finaloutput = new ArrayList<>();
        	Iterables.addAll(finaloutput, result);
        	Iterables.addAll(finaloutput, toplevel.changed());
		    result = ConfigBody.of(result.source(), finaloutput);
    	}
    	
    	return input.equals(result) ? input : result;
    }   
	
}
