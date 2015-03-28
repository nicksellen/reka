package reka.core.setup;

import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import reka.Identity;
import reka.PortRequirement;
import reka.api.IdentityStore;
import reka.api.Path;
import reka.api.data.Data;
import reka.api.flow.Flow;
import reka.core.app.Application;
import reka.core.app.LifecycleComponent;
import reka.core.builder.FlowVisualizer;
import reka.core.builder.Flows;
import reka.core.setup.ModuleSetup.ApplicationCheck;

public class ApplicationSetup {

	public final Flow initializationFlow;
	public final FlowVisualizer initializationFlowVisualizer;
	
	public final Map<Path, FlowSegmentBiFunction> providers;
	public final List<InitFlow> initflows;
	public final List<TriggerCollection> triggers;
	public final List<NetworkInfo> network;
	public final List<LifecycleComponent> components;
	public final List<Supplier<StatusProvider>> statuses;
	public final List<Consumer<ApplicationCheck>> checks;
	public final List<PortRequirement> networkRequirements;

	private Identity identity;
	private Path name;
	private Data meta;
	private int version = -1;
	private Flows flows;
	private IdentityStore store;

	ApplicationSetup(Flow initializationFlow, FlowVisualizer initializationFlowVisualizer, ModuleCollector collector) {
		this.initializationFlow = initializationFlow;
		this.initializationFlowVisualizer = initializationFlowVisualizer;
		providers = collector.providers;
		initflows = collector.initflows;
		triggers = collector.triggers;
		network = collector.network;
		components = collector.components;
		statuses = collector.statuses;
		checks = collector.checks;
		networkRequirements = collector.networkRequirements;
	}
	
	public void identity(Identity identity) {
		this.identity = identity;
	}
	
	public void name(Path name) {
		this.name = name;
	}
	
	public Path name() {
		return name;
	}
	
	public void meta(Data meta) {
		this.meta = meta;
	}
	
	public void version(int version) {
		this.version = version;
	}
	
	public int version() {
		return version;
	}
	
	public void flows(Flows flows) {
		this.flows = flows;
	}
	
	public void store(IdentityStore store) {
		this.store = store;
	}
	
	public Application buildApplication() {
		return new Application(identity,
							   name, 
							   meta, 
							   version, 
							   flows,
							   store != null ? store : IdentityStore.createConcurrentIdentityStore(),
							   network, 
							   initializationFlowVisualizer,
							   components,
							   statuses.stream().map(Supplier::get).collect(toList()));
	}

}