package reka.core.config;

import static reka.api.Path.dots;
import static reka.api.content.Contents.utf8;
import reka.api.Path;
import reka.api.data.Data;
import reka.api.data.MutableData;
import reka.config.Config;
import reka.core.data.memory.MutableMemoryData;

public class ConfigUtils {

	public static Data configToData(Config config) {
		return addConfigToData(config, MutableMemoryData.create()).readonly();
	}
	
	public static Data configToData(Iterable<Config> body) {
		MutableData data = MutableMemoryData.create();
		for (Config child : body) {
			addConfigToData(child, data);
		}
		return data.readonly();	
	}

	private static MutableData addConfigToData(Config config, MutableData data) {
		Path path = dots(config.key());
		if (config.hasBody()) {
			MutableData next = data.createMapAt(path);
			for (Config child : config.body()) {
				addConfigToData(child, next);
			}
		} else if (config.hasValue()) {
			String value = config.valueAsString();
			if ("{}".equals(value)) {
				data.createMapAt(path);
			} else { 
				data.put(path, utf8(value));
			}
		} else if (config.hasDocument()) {
			data.putString(path, config.documentContentAsString());
		} else {
			String key = config.key();
			if (key.startsWith("!")) {
				data.putBool(dots(key.substring(1)), false);
			} else {
				data.putBool(path, true);
			}
		}
		return data;
	}
	
}
