package reka.module.setup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import reka.api.Path;
import reka.app.ApplicationComponent;
import reka.module.PortRequirement;
import reka.module.setup.AppSetup.ApplicationCheck;

public class ModuleCollector {

	public final Map<Path, FlowSegmentBiFunction> providers;
	public final List<InitFlow> initflows;
	public final List<TriggerCollection> triggers;
	public final List<NetworkInfo> network;
	public final List<ApplicationComponent> components;
	public final List<Supplier<StatusProvider>> statuses;
	public final List<Consumer<ApplicationCheck>> checks;
	public final List<PortRequirement> networkRequirements;
	
	public ModuleCollector() {
		providers = new HashMap<>();
		initflows = new ArrayList<>();
		triggers = new ArrayList<>();
		network = new ArrayList<>();
		components = new ArrayList<>();
		statuses = new ArrayList<>();
		checks = new ArrayList<>();
		networkRequirements = new ArrayList<>();
	}

}