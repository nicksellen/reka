package reka.config.processor;

import java.util.Arrays;
import java.util.List;

import reka.config.Config;
import reka.config.ConfigBody;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.SetMultimap;

public final class MultiConverter implements ConfigConverter {
    
    private final List<ConfigConverter> converters;
    
    private final SetMultimap<ConfigConverter, Config> converted = HashMultimap.create();

    public MultiConverter(ConfigConverter... converters) {
        this(Arrays.asList(converters));
    }
    
    public MultiConverter(Iterable<ConfigConverter> converters) {
        this.converters = ImmutableList.copyOf(converters);
    }

    private ConfigBody internalConvert(Output toplevel, ConfigConverter converter, ConfigBody body, String[] path) {
        ProcessorOutput out = new ProcessorOutput(toplevel, new ConvertedSource(converter, body.source()), path);
        for (Config config : body) {
            if (converted.containsEntry(converter, config)) {
                out.add(config);
            } else {
                out.mark();
                converter.convert(config, out);
                converted.putAll(converter, out.changed());
            }
        }
        return ConfigBody.of(body.source(), out.configs());
    }

    @Override
    public void convert(Config config, Output out) {
        ConfigBody current = ConfigBody.of(config.source(), config);
        for (ConfigConverter converter : converters) {
            current = internalConvert(out.toplevel(), converter, current, out.path());
        }
        out.add(current);
    }
}