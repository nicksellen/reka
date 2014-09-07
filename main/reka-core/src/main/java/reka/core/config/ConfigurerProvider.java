package reka.core.config;

import java.util.Collection;
import java.util.function.Supplier;

import reka.api.Path;
import reka.api.flow.FlowSegment;
import reka.config.Config;

public interface ConfigurerProvider {
	public Collection<Path> types();
    public Supplier<FlowSegment> provide(String type, ConfigurerProvider provider, Config config);
}