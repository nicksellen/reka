package reka.core.setup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import reka.PortRequirement;
import reka.api.Path;
import reka.core.app.LifecycleComponent;
import reka.core.setup.ModuleSetup.ApplicationCheck;

public class ModuleCollector {

	public final Map<Path, FlowSegmentBiFunction> providers;
	public final List<InitFlow> initflows;
	public final List<TriggerCollection> triggers;
	public final List<Runnable> onUndeploy;
	public final List<NetworkInfo> network;
	public final List<LifecycleComponent> components;
	public final List<Supplier<StatusProvider>> statuses;
	public final List<Consumer<ApplicationCheck>> checks;
	public final List<PortRequirement> networkRequirements;
	
	public ModuleCollector() {
		providers = new HashMap<>();
		initflows = new ArrayList<>();
		triggers = new ArrayList<>();
		onUndeploy = new ArrayList<>();
		network = new ArrayList<>();
		components = new ArrayList<>();
		statuses = new ArrayList<>();
		checks = new ArrayList<>();
		networkRequirements = new ArrayList<>();
	}

}