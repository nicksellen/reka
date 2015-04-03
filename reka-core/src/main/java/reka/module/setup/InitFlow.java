package reka.module.setup;

import java.util.function.Consumer;
import java.util.function.Function;

import reka.api.Path;
import reka.core.config.ConfigurerProvider;
import reka.flow.Flow;

public class InitFlow {
	
	public final Path name;
	public final Function<ConfigurerProvider, OperationConfigurer> supplier;
	public final ModuleSetupContext ctx;
	public final Consumer<Flow> consumer;
	
	public InitFlow(Path name,
			Function<ConfigurerProvider, OperationConfigurer> supplier,
			ModuleSetupContext ctx,
			Consumer<Flow> consumer) {
		this.name = name;
		this.supplier = supplier;
		this.ctx = ctx;
		this.consumer = consumer;
	}
	
}