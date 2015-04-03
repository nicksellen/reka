package reka.module.setup;

import static java.util.stream.Collectors.toList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import reka.app.Application;
import reka.app.ApplicationComponent;
import reka.data.Data;
import reka.flow.Flow;
import reka.flow.builder.FlowVisualizer;
import reka.flow.builder.Flows;
import reka.identity.Identity;
import reka.identity.IdentityStore;
import reka.identity.IdentityStoreReader;
import reka.module.PortRequirement;
import reka.module.setup.AppSetup.ApplicationCheck;
import reka.util.Path;

import com.google.common.collect.ImmutableMap;

public class ApplicationSetup {

	public final Flow initializationFlow;
	public final FlowVisualizer initializationFlowVisualizer;
	
	public final Map<Path, FlowSegmentBiFunction> providers;
	public final List<InitFlow> initflows;
	public final List<TriggerCollection> triggers;
	public final List<NetworkInfo> network;
	public final List<ApplicationComponent> components;
	public final List<Supplier<StatusProvider>> statuses;
	public final List<Consumer<ApplicationCheck>> checks;
	public final List<PortRequirement> networkRequirements;

	private Identity identity;
	private Path name;
	private Data meta;
	private int version = -1;
	private Flows flows;
	
	private final Map<Path,IdentityStoreReader> stores = new HashMap<>();

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
	
	public void registerStore(Path path, IdentityStore store) {
		stores.put(path, store);
	}
	
	public Application buildApplication() {
		return new Application(identity,
							   name, 
							   meta, 
							   version, 
							   flows,
							   ImmutableMap.copyOf(stores),
							   network, 
							   initializationFlowVisualizer,
							   components,
							   statuses.stream().map(Supplier::get).collect(toList()));
	}

}