package reka.flow.builder;

import static reka.core.config.ConfigUtils.combine;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import reka.api.IdentityKey;
import reka.config.ConfigBody;
import reka.core.config.ConfigurerProvider;
import reka.flow.Flow;
import reka.module.setup.OperationConfigurer;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

public class TriggerHelper {

	private final ListMultimap<IdentityKey<Flow>,ConfigBody> triggers = ArrayListMultimap.create();
	
	public TriggerHelper add(IdentityKey<Flow> key, ConfigBody body) {
		triggers.put(key, body);
		return this;
	}
	
	public Map<IdentityKey<Flow>,Function<ConfigurerProvider, OperationConfigurer>> build() {
		Map<IdentityKey<Flow>,Function<ConfigurerProvider, OperationConfigurer>> result = new HashMap<>();
		triggers.asMap().forEach((key, bodies) -> {
			result.put(key, combine(bodies));
		}); 
		return result;
	}

	public boolean isEmpty() {
		return triggers.isEmpty();
	}
	
	public Set<IdentityKey<Flow>> keySet() {
		return triggers.keySet();
	}
	
}
