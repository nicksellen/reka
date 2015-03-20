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
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import reka.PortRequirement;
import reka.api.IdentityKey;
import reka.api.IdentityStore;
import reka.api.Path;
import reka.api.flow.Flow;
import reka.api.flow.FlowSegment;
import reka.api.run.Operation;
import reka.config.Config;
import reka.config.ConfigBody;
import reka.core.app.IdentityAndVersion;
import reka.core.config.ConfigurerProvider;
import reka.core.config.SequenceConfigurer;
import reka.core.module.ModuleInfo;
import reka.core.setup.ModuleConfigurer.ModuleCollector;

public class ModuleSetup {
	
	private final IdentityAndVersion idv;
	private final ModuleInfo info;
	private final ModuleCollector collector;
	private final Path path;
	private final ModuleSetupContext ctx;
	private final List<Supplier<FlowSegment>> segments = new ArrayList<>();	
	
	private boolean includeDefaultStatus = true;
	
	public ModuleSetup(IdentityAndVersion idv, ModuleInfo info, Path path, ModuleSetupContext ctx, ModuleCollector collector) {
		this.idv = idv;
		this.info = info;
		this.path = path;
		this.ctx = ctx;
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
		OperationSetup e = new SequentialCollector(path, ctx);
		init.accept(new ModuleOperationSetup(idv, e));
		segments.add(e);
		return this;
	}
	
	public void onShutdown(String name, Consumer<ModuleSetupContext> handler) {
		collector.shutdownHandlers.add(() -> handler.accept(ctx));
	}
	
	public void onShutdown(String name, BiConsumer<IdentityAndVersion, IdentityStore> handler) {
		collector.shutdownHandlers.add(() -> handler.accept(idv, ctx));
	}
	
	public ModuleSetup simpleOperation(Path name, Function<Config,Operation> fn) {
		return this;
	}
	
	public ModuleSetup operation(Path name, Function<ConfigurerProvider,OperationConfigurer> c) {
		
		FlowSegmentBiFunction f = (provider, config) -> {
			return configure(c.apply(provider), config).bind(path, ctx);
		};
		
		// register it under two names, the full path (e.g. net/http/router):
		collector.providers.put(path.add(name), f);
		
		if (!path.isEmpty()) {
			// and the short name (e.g. http/router):
			collector.providers.put(Path.path(path.last()).add(name), f);
		}
		
		return this;
	}
	
	public void status(Function<IdentityStore, StatusDataProvider> c) {
		includeDefaultStatus = false;
		collector.statuses.add(() -> StatusProvider.create(info.name().slashes(), path.slashes(), info.version(), c.apply(ctx)));
	}
	
	public ModuleSetup initflow(String name, ConfigBody body, Consumer<InitFlowSetup> init) {
		Function<ConfigurerProvider, OperationConfigurer> supplier = provider -> configure(new SequenceConfigurer(provider), body);
		collector.initflows.add(new InitFlow(path.add(name), supplier, ctx, flow -> {
			init.accept(new InitFlowSetup(flow, ctx));
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
	
	public ModuleSetup triggersWithBodies(Map<IdentityKey<Flow>,ConfigBody> bodies, Consumer<MultiFlowRegistration> cs) {
		collector.triggers.add(new TriggerCollection(bodies.entrySet().stream().map(e -> {
			return new Trigger(path, e.getKey(), provider -> configure(new SequenceConfigurer(provider), e.getValue()));
		}).collect(toList()), cs, ctx));
		return this;
	}
	
	public ModuleSetup triggers(Map<IdentityKey<Flow>,Function<ConfigurerProvider, OperationConfigurer>> suppliers, Consumer<MultiFlowRegistration> cs) {
		collector.triggers.add(new TriggerCollection(suppliers.entrySet().stream().map(e -> new Trigger(path, e.getKey(), e.getValue())).collect(toList()), cs, ctx));
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

	public void requirePort(int port, Optional<String> host) {
		collector.portRequirements.add(new PortRequirement(port, host));
	}
	
	public void requirePort(int port) {
		requirePort(port, Optional.empty());
	}
	
}