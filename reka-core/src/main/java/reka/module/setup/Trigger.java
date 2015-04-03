package reka.module.setup;

import java.util.function.Function;

import reka.core.config.ConfigurerProvider;
import reka.flow.Flow;
import reka.identity.IdentityKey;
import reka.util.Path;

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