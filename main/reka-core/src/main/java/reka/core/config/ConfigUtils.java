package reka.core.config;

import static reka.api.Path.dots;
import static reka.api.Path.root;
import static reka.api.content.Contents.falseValue;
import static reka.api.content.Contents.trueValue;
import static reka.api.content.Contents.utf8;
import static reka.config.configurer.Configurer.Preconditions.checkConfig;
import reka.api.Path;
import reka.api.data.Data;
import reka.api.data.MutableData;
import reka.config.Config;
import reka.core.data.memory.MutableMemoryData;

public class ConfigUtils {

	public static Data configToData(Config config) {
		return addConfigToData(config, MutableMemoryData.create(), root()).immutable();
	}
	
	public static Data configToData(Iterable<Config> body) {
		MutableData data = MutableMemoryData.create();
		for (Config child : body) {
			addConfigToData(child, data, root());
		}
		return data.immutable();	
	}

	private static MutableData addConfigToData(Config config, MutableData data, Path base) {
		Path path = base.add(dots(config.key()));
		if (config.hasBody()) {
			if (config.hasValue()) {
				data.putOrAppend(path.add("@"), utf8(config.valueAsString()));
			}
			for (Config child : config.body()) {
				addConfigToData(child, data, path);
			}
		} else if (config.hasValue()) {
			data.putOrAppend(path, utf8(config.valueAsString()));
		} else if (config.hasDocument()) {
			checkConfig(!config.hasValue(), "you can't include a value and a document");
			data.putOrAppend(path, utf8(config.documentContentAsString()));
		} else {
			String key = config.key();
			if (key.startsWith("!")) {
				data.putOrAppend(dots(key.substring(1)), falseValue());
			} else {
				data.putOrAppend(path, trueValue());
			}
		}
		return data;
	}
	
}
