package reka.core.config;

import static java.util.stream.Collectors.toList;
import static reka.api.Path.dots;
import static reka.config.configurer.Configurer.configure;
import static reka.config.configurer.Configurer.Preconditions.checkConfig;
import static reka.core.builder.FlowSegments.meta;
import static reka.core.builder.FlowSegments.seq;
import static reka.util.Util.createEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Supplier;

import reka.api.data.MutableData;
import reka.api.flow.FlowSegment;
import reka.config.Config;
import reka.config.configurer.annotations.Conf;
import reka.core.data.memory.MutableMemoryData;


public class SequenceConfigurer implements Supplier<FlowSegment> {

    private final ConfigurerProvider provider;

    public SequenceConfigurer(ConfigurerProvider provider) {
        this.provider = provider;
    }

    private final List<Entry<Config,Supplier<FlowSegment>>> configurers = new ArrayList<>();

    @Conf.Each
    public void each(Config config) {
        Optional<Supplier<FlowSegment>> configurer = provider.provide(config.key(), provider);
        checkConfig(configurer.isPresent(), "no configurer for [%s]", config.key());
        configurers.add(createEntry(config, configure(configurer.get(), config)));
    }

    @Override
    public FlowSegment get() {
    	return seq(configurers.stream().map(e -> {
    		Config config = e.getKey();
    		
    		MutableData meta = MutableMemoryData.create()
        			.putString("key", config.key());
    		
    		if (config.hasValue()) {
    			meta.putString("value", config.valueAsString());
    		}
    		
    		meta.putInt(dots("location.start.line"), config.source().linenumbers().startLine());
    		meta.putInt(dots("location.start.pos"), config.source().linenumbers().startPos());
    		meta.putInt(dots("location.end.line"), config.source().linenumbers().endLine());
    		meta.putInt(dots("location.end.pos"), config.source().linenumbers().endPos());
    		
    		if (config.source().origin().isFile()) {
    			meta.putString("filename", config.source().origin().file().getAbsolutePath());
    		}
    		
    		
    		return meta(e.getValue().get(), meta);
    		
    	}).collect(toList()));
    }

}
