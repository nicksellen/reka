package reka.config.processor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import reka.config.Config;
import reka.config.ConfigBody;

public final class MultiConverter implements ConfigConverter {
    
    private final List<ConfigConverter> converters;
    
    private final Map<ConfigConverter,Set<Config>> converted = new HashMap<>();

    public MultiConverter(ConfigConverter... converters) {
        this(Arrays.asList(converters));
    }
    
    public MultiConverter(Collection<ConfigConverter> converters) {
        this.converters = new ArrayList<>(converters);
    }

    private ConfigBody internalConvert(Output toplevel, ConfigConverter converter, ConfigBody body, int depth) {
        ProcessorOutput out = new ProcessorOutput(toplevel, new ConvertedSource(converter, body.source()), depth);
        for (Config config : body) {
        	if (converted.containsKey(converter) && converted.get(converter).contains(config)) {
                out.add(config);
            } else {
                out.mark();
                converter.convert(config, out);
                converted.putIfAbsent(converter, new HashSet<>());
                converted.get(converter).addAll(out.changed());
                
            }
        }
        return ConfigBody.of(body.source(), out.configs());
    }

    @Override
    public void convert(Config config, Output out) {
        ConfigBody current = ConfigBody.of(config.source(), config);
        for (ConfigConverter converter : converters) {
            current = internalConvert(out.toplevel(), converter, current, out.depth());
        }
        out.add(current);
    }
}