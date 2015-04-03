package reka.module.setup;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static reka.config.configurer.Configurer.configure;
import static reka.flow.builder.FlowSegments.createLabelSegment;
import static reka.flow.builder.FlowSegments.seq;
import static reka.util.Util.unsupported;

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

import reka.api.IdentityKey;
import reka.api.IdentityStore;
import reka.api.Path;
import reka.app.ApplicationComponent;
import reka.app.IdentityAndVersion;
import reka.config.Config;
import reka.config.ConfigBody;
import reka.core.config.ConfigurerProvider;
import reka.core.config.SequenceConfigurer;
import reka.data.MutableData;
import reka.data.memory.MutableMemoryData;
import reka.flow.Flow;
import reka.flow.FlowSegment;
import reka.flow.ops.Operation;
import reka.module.ModuleInfo;
import reka.module.PortRequirement;
import reka.util.Identity;

public class AppSetup {
	
	private final IdentityAndVersion idv;
	private final ModuleInfo info;
	private final ModuleCollector collector;
	private final Path path;
	private final ModuleSetupContext ctx;
	private final List<Supplier<FlowSegment>> segments = new ArrayList<>();	
	
	private boolean includeDefaultStatus = true;
	
	public AppSetup(IdentityAndVersion idv, ModuleInfo info, Path path, ModuleSetupContext ctx, ModuleCollector collector) {
		this.idv = idv;
		this.info = info;
		this.path = path;
		this.ctx = ctx;
		this.collector = collector;
	}
	
	protected boolean includeDefaultStatus() {
		return includeDefaultStatus;
	}
	
	public Identity identity() {
		return idv.identity();
	}
	
	public int version() {
		return idv.version();
	}
	
	public ModuleSetupContext ctx() {
		return ctx;
	}
	
	public Path path() {
		return path;
	}

	public void requireNetwork(int port, String host) {
		collector.networkRequirements.add(new PortRequirement(port, Optional.of(host)));
	}
	
	public void requireNetwork(int port) {
		collector.networkRequirements.add(new PortRequirement(port, Optional.empty()));
	}
	
	public void registerComponent(ApplicationComponent component) {
		collector.components.add(component);
	}
	
	public void registerNetwork(int port, String protocol) {
		registerNetwork(port, protocol, data -> {});
	}
	
	public void registerNetwork(int port, String protocol, Consumer<MutableData> details) {
		MutableData data = MutableMemoryData.create();
		details.accept(data);
		collector.network.add(new NetworkInfo(port, protocol, data.immutable()));
	}
	
	public void onDeploy(Consumer<ModuleOperationSetup> init) {
		OperationSetup e = new SequentialCollector(path, ctx);
		init.accept(new ModuleOperationSetup(idv, e));
		segments.add(e);
	}
	
	public void onUndeploy(String name, Runnable runnable) {
		collector.components.add(new ApplicationComponent(){

			@Override
			public void undeploy() {
				runnable.run();
			}

			@Override
			public Runnable pause() {
				return () -> {};
			}
			
		});
	}
	
	public void onUndeploy(String name, BiConsumer<IdentityAndVersion, IdentityStore> handler) {
		onUndeploy(name, () -> handler.accept(idv, ctx));
	}
	
	public void simpleOperation(Path name, Function<Config,Operation> fn) {
		throw unsupported("I haven't implemented this yet, it wouldn't be very tricky though, just gotta mush some things about");
	}
	
	public void defineOperation(Path name, Function<ConfigurerProvider,OperationConfigurer> c) {
		
		FlowSegmentBiFunction f = (provider, config) -> {
			return configure(c.apply(provider), config).bind(path, ctx);
		};
		
		// register it under two names, the full path (e.g. net/http/router):
		collector.providers.put(path.add(name), f);
		
		if (!path.isEmpty()) {
			// and the short name (e.g. http/router):
			collector.providers.put(Path.path(path.last()).add(name), f);
		}
	}
	
	public void registerStatusProvider(Supplier<StatusDataProvider> c) {
		includeDefaultStatus = false;
		collector.statuses.add(() -> StatusProvider.create(info.name().slashes(), path.slashes(), info.version(), c.get()));
	}
	
	public void buildInitializationFlow(String name, ConfigBody body, Consumer<Flow> init) {
		Function<ConfigurerProvider, OperationConfigurer> supplier = provider -> configure(new SequenceConfigurer(provider), body);
		collector.initflows.add(new InitFlow(path.add(name), supplier, ctx, flow -> {
			init.accept(flow);
		}));
	}
	
	public static class ApplicationCheck {
		
		private final Identity identity;
		private final List<String> errors = new ArrayList<>();
		
		public ApplicationCheck(Identity identity) {
			this.identity = identity;
		}
		
		public Identity identity() {
			return identity;
		}
		
		public void error(String msg, Object... objs) {
			errors.add(format(msg, objs));
		}
		
		public List<String> errors() {
			return Collections.unmodifiableList(errors);
		}
		
	}

	public AppSetup check(Consumer<ApplicationCheck> check) {
		collector.checks.add(check);
		return this;
	}
	
	public void buildFlow(String name, ConfigBody body, Consumer<Flow> c) {
		buildFlow(IdentityKey.named(name), body, c);
	}
	
	private void buildFlow(IdentityKey<Flow> key, ConfigBody body, Consumer<Flow> c) {
		buildFlow(key, provider -> configure(new SequenceConfigurer(provider), body), c);
	}
	 
	public void buildFlow(String name, Function<ConfigurerProvider, OperationConfigurer> supplier, Consumer<Flow> c) {
		buildFlow(IdentityKey.named(name), supplier, c);
	}

	public void buildFlows(Map<IdentityKey<Flow>,Function<ConfigurerProvider, OperationConfigurer>> suppliers, Consumer<TriggerFlows> cs) {
		collector.triggers.add(new TriggerCollection(suppliers.entrySet().stream().map(e -> new Trigger(path, e.getKey(), e.getValue())).collect(toList()), cs, ctx));
	}

	private void buildFlow(IdentityKey<Flow> key, Function<ConfigurerProvider, OperationConfigurer> supplier, Consumer<Flow> c) {
		Map<IdentityKey<Flow>,Function<ConfigurerProvider, OperationConfigurer>> suppliers = new HashMap<>();
		suppliers.put(key, supplier);
		buildFlows(suppliers, m -> c.accept(m.lookup(key).get()));
	}
	
	protected Optional<FlowSegment> buildFlowSegment() {
		if (segments.isEmpty()) return Optional.empty();
		List<FlowSegment> built = segments.stream().map(Supplier<FlowSegment>::get).collect(toList());
		return Optional.of(createLabelSegment(path.slashes(), seq(built)));
	}

	public static interface DoneCallback extends Runnable {
		void done();
		default void run() {
			done();
		}
	}

	
}