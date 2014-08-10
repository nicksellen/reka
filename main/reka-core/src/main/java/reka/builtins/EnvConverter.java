package reka.builtins;

import static reka.config.configurer.Configurer.Preconditions.checkConfig;
import static reka.util.Util.createEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import reka.config.Config;
import reka.config.ConfigBody;
import reka.config.processor.ConfigConverter;

import com.google.common.base.Splitter;

public class EnvConverter implements ConfigConverter {
	
	static final Splitter splitter = Splitter.on(" ").trimResults();
	
	@Override
	public void convert(Config config, Output out) {
		if (config.hasKey() && config.key().equals("@env")) {
			
			checkConfig(config.hasValue(), "must have value");
			
			String envVar = config.valueAsString();

			Map<String, String> env = System.getenv();
			String envVal = env.get(envVar);
			
			if (config.hasBody()) {
				
				List<Entry<String,ConfigBody>> cases = new ArrayList<>();
				List<ConfigBody> otherwises = new ArrayList<>();
				
				for (Config c : config.body()) {
					
					if (!c.hasBody()) continue;
					
					switch (c.key()) {
					case "when":
						if (c.hasValue()) {
							cases.add(createEntry(c.valueAsString(), c.body()));
						}
						break;
					case "otherwise":
						otherwises.add(c.body());
						break;
					}
					
				}
						
				boolean caseMatch = false;
				
				for (Entry<String, ConfigBody> c : cases) {
					for (String e : splitter.split(c.getKey())) {
						if (e.equals(envVal)) {
							out.add(c.getValue());
							caseMatch = true;
							break;
						}
					}
				}
				
				if (!caseMatch) {
					for (ConfigBody e : otherwises) {
						out.add(e);
					}
				}
			}
			
		} else {
			out.add(config);
			return;
		}	
	}

}
