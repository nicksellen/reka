package reka.core.setup;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static reka.config.configurer.Configurer.configure;
import static reka.core.builder.FlowSegments.createLabelSegment;
import static reka.core.builder.FlowSegments.seq;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import reka.api.IdentityKey;
import reka.api.IdentityStore;
import reka.api.Path;
import reka.api.flow.Flow;
import reka.api.flow.FlowSegment;
import reka.config.ConfigBody;
import reka.core.config.ConfigurerProvider;
import reka.core.config.SequenceConfigurer;
import reka.core.module.ModuleInfo;
import reka.core.setup.ModuleConfigurer.ModuleCollector;

public class ModuleSetup {
	
	private final ModuleInfo info;
	private final ModuleCollector collector;
	private final Path path;
	private final IdentityStore store;
	private final List<Supplier<FlowSegment>> segments = new ArrayList<>();	
	
	private boolean includeDefaultStatus = true;
	
	public ModuleSetup(ModuleInfo info, Path path, IdentityStore store, ModuleCollector collector) {
		this.info = info;
		this.path = path;
		this.store = store;
		this.collector = collector;
	}
	
	protected boolean includeDefaultStatus() {
		return includeDefaultStatus;
	}
	
	public Path path() {
		return path;
	}
	
	public static interface DoneCallback extends Runnable {
		void done();
		default void run() {
			done();
		}
	}
	
	public ModuleSetup setupInitializer(Consumer<ModuleOperationSetup> init) {
		OperationSetup e = new SequentialCollector(path, store);
		init.accept(new ModuleOperationSetup(e));
		segments.add(e);
		return this;
	}
	
	public void onShutdown(String name, Consumer<IdentityStore> handler) {
		collector.shutdownHandlers.add(() -> handler.accept(store));
	}
	
	public ModuleSetup operation(Path name, Function<ConfigurerProvider,OperationConfigurer> c) {
		collector.providers.put(path.add(name), (provider, config) -> {
			return configure(c.apply(provider), config).bind(path, store);
		});
		return this;
	}
	
	public void status(Function<IdentityStore, StatusDataProvider> c) {
		includeDefaultStatus = false;
		collector.statuses.add(() -> StatusProvider.create(info.name().slashes(), path.slashes(), info.version(), c.apply(store)));
	}
	
	public ModuleSetup initflow(String name, ConfigBody body, Consumer<InitFlowSetup> init) {
		Function<ConfigurerProvider, OperationConfigurer> supplier = provider -> configure(new SequenceConfigurer(provider), body);
		collector.initflows.add(new InitFlow(path.add(name), supplier, store, flow -> {
			init.accept(new InitFlowSetup(flow, store));
		}));
		return this;
	}
	
	public static class ApplicationCheck {
		
		private final String applicationIdentity;
		private final List<String> errors = new ArrayList<>();
		
		public ApplicationCheck(String applicationIdentity) {
			this.applicationIdentity = applicationIdentity;
		}
		
		public String applicationIdentity() {
			return applicationIdentity;
		}
		
		public void error(String msg, Object... objs) {
			errors.add(format(msg, objs));
		}
		
		public List<String> errors() {
			return Collections.unmodifiableList(errors);
		}
		
	}

	public ModuleSetup check(Consumer<ApplicationCheck> check) {
		collector.checks.add(check);
		return this;
	}
	
	public ModuleSetup trigger(String name, ConfigBody body, Consumer<SingleFlowRegistration> c) {
		return trigger(IdentityKey.named(name), body, c);
	}

	public ModuleSetup trigger(String name, Function<ConfigurerProvider, OperationConfigurer> supplier, Consumer<SingleFlowRegistration> c) {
		return trigger(IdentityKey.named(name), supplier, c);
	}
	
	public ModuleSetup triggers(Map<IdentityKey<Flow>,Function<ConfigurerProvider, OperationConfigurer>> suppliers, Consumer<MultiFlowRegistration> cs) {
		collector.triggers.add(new TriggerCollection(suppliers.entrySet().stream().map(e -> new Trigger(path, e.getKey(), e.getValue())).collect(toList()), cs, store));
		return this;
	}

	private ModuleSetup trigger(IdentityKey<Flow> key, ConfigBody body, Consumer<SingleFlowRegistration> c) {
		return trigger(key, provider -> configure(new SequenceConfigurer(provider), body), c);
	}
	
	private ModuleSetup trigger(IdentityKey<Flow> key, Function<ConfigurerProvider, OperationConfigurer> supplier, Consumer<SingleFlowRegistration> c) {
		Map<IdentityKey<Flow>,Function<ConfigurerProvider, OperationConfigurer>> suppliers = new HashMap<>();
		suppliers.put(key, supplier);
		return triggers(suppliers, m -> c.accept(m.singleFor(key)));
	}
	
	protected Optional<FlowSegment> buildFlowSegment() {
		if (segments.isEmpty()) return Optional.empty();
		List<FlowSegment> built = segments.stream().map(Supplier<FlowSegment>::get).collect(toList());
		return Optional.of(createLabelSegment(path.slashes(), seq(built)));
	}
	
}