package reka.core.setup;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

import reka.api.IdentityStore;

public class TriggerCollection {
	
	private final List<Trigger> triggers;
	private final Consumer<MultiFlowRegistration> consumer;
	private final IdentityStore store;
	
	public TriggerCollection(Collection<Trigger> triggers, Consumer<MultiFlowRegistration> consumer, IdentityStore store) {
		this.triggers = new ArrayList<>(triggers);
		this.consumer = consumer;
		this.store = store;
	}
	
	public List<Trigger> get() {
		return triggers;
	}
	
	public Consumer<MultiFlowRegistration> consumer() {
		return consumer;
	}
	
	public IdentityStore store() {
		return store;
	}
	
}