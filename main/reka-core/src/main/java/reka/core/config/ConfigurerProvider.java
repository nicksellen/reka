package reka.core.config;

import java.util.Collection;
import java.util.Optional;
import java.util.function.Supplier;

import reka.api.Path;
import reka.api.flow.FlowSegment;

public interface ConfigurerProvider {
	public Collection<Path> types();
    public Optional<Supplier<FlowSegment>> provide(String type, ConfigurerProvider provider);
}