package reka.core.setup;

import java.util.ArrayList;
import java.util.Map;

import reka.api.IdentityKey;
import reka.api.IdentityStore;
import reka.api.flow.Flow;

public class MultiFlowRegistration extends BaseRegistration {
	
	private final Map<IdentityKey<Flow>,Flow> map;
	
	public MultiFlowRegistration(int applicationVersion, String identity, IdentityStore store, Map<IdentityKey<Flow>,Flow> map) {
		super(applicationVersion, identity, store, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
		this.map = map;
	}

	public boolean has(IdentityKey<Flow> name) {
		return map.containsKey(name);
	}
	
	public Flow get(IdentityKey<Flow> name) {
		return map.get(name);
	}
	
	public SingleFlowRegistration singleFor(IdentityKey<Flow> name) {
		return new SingleFlowRegistration(this, get(name));
	}
	
}