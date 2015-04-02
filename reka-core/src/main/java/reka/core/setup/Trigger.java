package reka.core.setup;

import java.util.function.Function;

import reka.api.IdentityKey;
import reka.api.Path;
import reka.api.flow.Flow;
import reka.core.config.ConfigurerProvider;

public class Trigger {

	private final Path base;
	private final IdentityKey<Flow> name;
	private final Function<ConfigurerProvider, OperationConfigurer> supplier;
	
	public Trigger(Path base, IdentityKey<Flow> name, Function<ConfigurerProvider, OperationConfigurer> supplier) {
		this.base = base;
		this.name = name;
		this.supplier = supplier;
	}

	public Path base() {
		return base;
	}
	
	public IdentityKey<Flow> key() {
		return name;
	}
	
	public Function<ConfigurerProvider,OperationConfigurer> supplier() {
		return supplier;
	}
	
}