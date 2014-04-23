package reka.core.config;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.stream.Collectors.joining;
import static reka.api.Path.slashes;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import reka.api.Path;
import reka.api.flow.FlowSegment;

import com.google.common.base.Optional;

public class MultiConfigurerProvider implements ConfigurerProvider {

	private final Map<Path, Function<ConfigurerProvider, Supplier<FlowSegment>>> providers;
	
	public MultiConfigurerProvider(Map<Path, Function<ConfigurerProvider, Supplier<FlowSegment>>> providers) {
		this.providers = providers;
	}
	
	@Override
	public Optional<Supplier<FlowSegment>> provide(String type, ConfigurerProvider provider) {
		Path typePath = slashes(type);
		Function<ConfigurerProvider, Supplier<FlowSegment>> p = providers.get(typePath);
		String typesWeCanMake = provider.types().stream().map(Path::slashes).collect(joining(", "));
		checkNotNull(p, "don't know how to make a [%s] (we can make [%s])", typePath.slashes(), typesWeCanMake);
		return Optional.fromNullable(p.apply(provider));
	}

	@Override
	public Collection<Path> types() {
		return providers.keySet();
	}

}
