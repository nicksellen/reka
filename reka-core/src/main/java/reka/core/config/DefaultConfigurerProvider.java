package reka.core.config;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.stream.Collectors.joining;
import static reka.util.Path.slashes;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import reka.config.Config;
import reka.flow.FlowSegment;
import reka.module.setup.FlowSegmentBiFunction;
import reka.util.Path;

public class DefaultConfigurerProvider implements ConfigurerProvider {

	private final Map<Path, FlowSegmentBiFunction> providers;
	
	public DefaultConfigurerProvider(Map<Path, FlowSegmentBiFunction> providers) {
		this.providers = providers;
	}
	
	@Override
	public Supplier<FlowSegment> provide(String type, ConfigurerProvider parentProvider, Config config) {
		Path typePath = slashes(type);
		BiFunction<ConfigurerProvider, Config, Supplier<FlowSegment>> p = providers.get(typePath);
		String typesWeCanMake = parentProvider.types().stream().map(Path::slashes).sorted().collect(joining(", "));
		checkNotNull(p, "don't know how to make a [%s] (we can make [%s])", typePath.slashes(), typesWeCanMake);
		return p.apply(parentProvider, config);
	}

	@Override
	public Collection<Path> types() {
		return providers.keySet();
	}

	@Override
	public ConfigurerProvider add(Path type, FlowSegmentBiFunction provider) {
		Map<Path,FlowSegmentBiFunction> newProviders = new HashMap<>(providers);
		newProviders.put(type, provider);
		return new DefaultConfigurerProvider(newProviders);
	}

}
