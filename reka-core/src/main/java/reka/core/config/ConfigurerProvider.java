package reka.core.config;

import java.util.Collection;
import java.util.function.Supplier;

import reka.config.Config;
import reka.flow.FlowSegment;
import reka.module.setup.FlowSegmentBiFunction;
import reka.util.Path;

public interface ConfigurerProvider {
	Collection<Path> types();
    Supplier<FlowSegment> provide(String type, ConfigurerProvider provider, Config config);
    ConfigurerProvider add(Path type, FlowSegmentBiFunction provider);
}