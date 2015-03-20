package reka.core.setup;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

public class TriggerCollection {
	
	private final List<Trigger> triggers;
	private final Consumer<MultiFlowRegistration> consumer;
	private final ModuleSetupContext ctx;
	
	public TriggerCollection(Collection<Trigger> triggers, Consumer<MultiFlowRegistration> consumer, ModuleSetupContext ctx) {
		this.triggers = new ArrayList<>(triggers);
		this.consumer = consumer;
		this.ctx = ctx;
	}
	
	public List<Trigger> get() {
		return triggers;
	}
	
	public Consumer<MultiFlowRegistration> consumer() {
		return consumer;
	}
	
	public ModuleSetupContext ctx() {
		return ctx;
	}
	
}