package reka.core.setup;

import reka.api.IdentityStore;
import reka.api.flow.Flow;

public class InitFlowSetup {
	
	private final Flow flow;
	private final IdentityStore store;
	
	public InitFlowSetup(Flow flow, IdentityStore store) {
		this.flow = flow;
		this.store = store;
	}
	
	public Flow flow() {
		return flow;
	}
	
	public IdentityStore store() {
		return store;
	}
}