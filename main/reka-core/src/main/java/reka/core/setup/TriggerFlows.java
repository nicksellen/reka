package reka.core.setup;

import java.util.Map;
import java.util.Optional;

import reka.Identity;
import reka.api.IdentityKey;
import reka.api.flow.Flow;

public class TriggerFlows {

	private final Map<IdentityKey<Flow>, Flow> map;

	public TriggerFlows(Identity identity, Map<IdentityKey<Flow>, Flow> map) {
		this.map = map;
	}

	public Optional<Flow> flowFor(IdentityKey<Flow> name) {
		return map.containsKey(name) ? Optional.of(map.get(name)) : Optional.empty();
	}

}