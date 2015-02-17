package reka.core.builder;

import static reka.core.config.ConfigUtils.combine;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import reka.api.IdentityKey;
import reka.api.flow.Flow;
import reka.config.ConfigBody;
import reka.core.config.ConfigurerProvider;
import reka.core.setup.OperationConfigurer;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

public class TriggerHelper {

	private final ListMultimap<IdentityKey<Flow>,ConfigBody> triggers = ArrayListMultimap.create();
	
	public TriggerHelper addTrigger(IdentityKey<Flow> key, ConfigBody body) {
		System.out.printf("addTrigger %s\n", key.name());
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
	
}
