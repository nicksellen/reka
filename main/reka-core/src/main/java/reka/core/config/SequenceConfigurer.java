package reka.core.config;

import static java.util.stream.Collectors.toList;
import static reka.configurer.Configurer.configure;
import static reka.configurer.Configurer.Preconditions.checkConfig;
import static reka.core.builder.FlowSegments.seq;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import reka.api.flow.FlowSegment;
import reka.config.Config;
import reka.configurer.annotations.Conf;

import com.google.common.base.Optional;

public class SequenceConfigurer implements Supplier<FlowSegment> {

    private final ConfigurerProvider provider;

    public SequenceConfigurer(ConfigurerProvider provider) {
        this.provider = provider;
    }

    private final List<Supplier<FlowSegment>> configurers = new ArrayList<>();

    @Conf.Each
    public void each(Config config) {
        Optional<Supplier<FlowSegment>> configurer = provider.provide(config.key(), provider);
        checkConfig(configurer.isPresent(), "no configurer for [%s]", config.key());
        configurers.add(configure(configurer.get(), config));
    }

    @Override
    public FlowSegment get() {
    	List<FlowSegment> segments = configurers.stream().map(Supplier<FlowSegment>::get).collect(toList());
        return seq(segments);
    }

}
