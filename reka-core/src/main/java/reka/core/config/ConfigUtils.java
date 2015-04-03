package reka.core.config;

import static reka.config.configurer.Configurer.configure;
import static reka.config.configurer.Configurer.Preconditions.checkConfig;
import static reka.data.content.Contents.booleanValue;
import static reka.data.content.Contents.utf8;
import static reka.util.Path.dots;
import static reka.util.Path.root;

import java.util.Collection;
import java.util.function.Function;

import reka.config.Config;
import reka.config.ConfigBody;
import reka.data.Data;
import reka.data.MutableData;
import reka.data.memory.MutableMemoryData;
import reka.module.setup.OperationConfigurer;
import reka.util.Path;

public class ConfigUtils {

	public static Function<ConfigurerProvider,OperationConfigurer> combine(Collection<ConfigBody> bodies) {
		return provider -> {
			return ops -> {
				ops.parallel(par -> {
					bodies.forEach(body -> {
						par.add(configure(new SequenceConfigurer(provider), body));
					});
				});
			};
		};
	}
	
	public static Data configToData(Config config) {
		Path path = config.hasKey() ? root().add(dots(config.key())) : root().add(0);
		return addConfigToData(config, MutableMemoryData.create(), path).immutable();
	}
	
	public static Data configToData(Iterable<Config> body) {
		return addConfigToData(body, MutableMemoryData.create(), root()).immutable();	
	}
	
	private static MutableData addConfigToData(Iterable<Config> body, MutableData data, Path path) {
		int idx = 0;
		for (Config child : body) {
			if (child.hasKey()) {
				addConfigToData(child, data, path.add(dots(child.key())));	
			} else {
				addConfigToData(child, data, path.add(idx));
				idx++;
			}
		}
		return data;
	}

	private static MutableData addConfigToData(Config config, MutableData data, Path path) {
		if (config.hasBody()) {
			if (config.hasValue()) {
				data.putString(path.add("@"), config.valueAsString());
			}
			addConfigToData(config.body(), data, path);
		} else if (config.hasValue()) {
			data.putOrAppend(path, utf8(config.valueAsString()));
		} else if (config.hasDocument()) {
			checkConfig(!config.hasValue(), "you can't include a value and a document");
			data.putOrAppend(path, utf8(config.documentContentAsString()));
		} else if (config.hasKey()) {
			String key = config.key();
			if (key.startsWith("!")) {
				data.putOrAppend(dots(key.substring(1)), booleanValue(false));
			} else {
				data.putOrAppend(path, booleanValue(true));
			}
		}
		return data;
	}
	
}
