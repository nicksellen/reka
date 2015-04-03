package reka.module.setup;

import java.util.Map;
import java.util.Optional;

import reka.api.IdentityKey;
import reka.flow.Flow;
import reka.util.Identity;

public class TriggerFlows {

	private final Map<IdentityKey<Flow>, Flow> map;

	public TriggerFlows(Identity identity, Map<IdentityKey<Flow>, Flow> map) {
		this.map = map;
	}

	public Optional<Flow> lookup(IdentityKey<Flow> name) {
		return map.containsKey(name) ? Optional.of(map.get(name)) : Optional.empty();
	}

}