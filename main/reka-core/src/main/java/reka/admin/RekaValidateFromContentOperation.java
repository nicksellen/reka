package reka.admin;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.map.ObjectMapper;

import reka.ApplicationManager;
import reka.api.Path;
import reka.api.data.MutableData;
import reka.api.run.RouteCollector;
import reka.api.run.RoutingOperation;
import reka.config.StringSource;
import reka.configurer.Configurer.InvalidConfigurationException;
import reka.core.data.memory.MutableMemoryData;
import reka.util.Util;

public class RekaValidateFromContentOperation implements RoutingOperation {
	
	private static final ObjectMapper jsonMapper = new ObjectMapper();
	
	private final ApplicationManager manager;
	private final Path in;
	
	public RekaValidateFromContentOperation(ApplicationManager manager, Path in) {
		this.manager = manager;
		this.in = in;
	}

	@Override
	public MutableData call(MutableData data, RouteCollector router) {
		try {
			String configString = UseReka.getConfigStringFromData(data, in);
			manager.validate(StringSource.from(configString));
			router.routeTo("ok");
			return data;
		} catch (Throwable t) {
			t = Util.unwrap(t);
			if (t instanceof InvalidConfigurationException) {
				InvalidConfigurationException e = (InvalidConfigurationException) t;
				try {
					
					Map<String,Object> map = new HashMap<>();
					map.put("errors", jsonMapper.readValue(e.toJson(), List.class));
					
					MutableMemoryData.createFromMap(map).forEachContent((p, c) -> {
						data.put(p, c);
					});
					
				} catch (IOException e1) {
					data.putString("error", e.getMessage());
				}
			} else {
				data.putString("error", t.getMessage());
			}
			router.routeTo("error");
		}
		return data;
	}
	
}