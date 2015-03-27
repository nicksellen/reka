package reka.core.setup;

import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;

import reka.Identity;
import reka.api.IdentityKey;
import reka.api.IdentityStore;
import reka.api.flow.Flow;

public class MultiFlowRegistration extends BaseRegistration {

	private final Map<IdentityKey<Flow>, Flow> map;

	public MultiFlowRegistration(int applicationVersion, Identity identity, IdentityStore store, Map<IdentityKey<Flow>, Flow> map) {
		super(applicationVersion, store, new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
		this.map = map;
	}

	public Optional<Flow> flowFor(IdentityKey<Flow> name) {
		return map.containsKey(name) ? Optional.of(map.get(name)) : Optional.empty();
	}

	public SingleFlowRegistration singleFor(IdentityKey<Flow> name) {
		return new SingleFlowRegistration(this, flowFor(name).get());
	}

}