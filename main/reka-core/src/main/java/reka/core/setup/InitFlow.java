package reka.core.setup;

import java.util.function.Consumer;
import java.util.function.Function;

import reka.api.IdentityStore;
import reka.api.Path;
import reka.api.flow.Flow;
import reka.core.config.ConfigurerProvider;

public class InitFlow {
	
	public final Path name;
	public final Function<ConfigurerProvider, OperationConfigurer> supplier;
	public final IdentityStore store;
	public final Consumer<Flow> consumer;
	
	public InitFlow(Path name,
			Function<ConfigurerProvider, OperationConfigurer> supplier,
			IdentityStore store,
			Consumer<Flow> consumer) {
		this.name = name;
		this.supplier = supplier;
		this.store = store;
		this.consumer = consumer;
	}
	
}