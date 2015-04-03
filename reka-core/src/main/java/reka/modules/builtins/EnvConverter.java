package reka.modules.builtins;

import static reka.config.configurer.Configurer.Preconditions.checkConfig;
import static reka.util.Util.createEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import reka.config.Config;
import reka.config.ConfigBody;
import reka.config.processor.ConfigConverter;

import com.google.common.base.Splitter;

public class EnvConverter implements ConfigConverter {
	
	private static final Pattern ENV = Pattern.compile("^@env\\(([^\\s\\(\\)]*)\\)$");
	
	static final Splitter splitter = Splitter.on(" ").trimResults().omitEmptyStrings();
	
	@Override
	public void convert(Config config, Output out) {
		if (config.hasKey()) {
			Matcher matcher = ENV.matcher(config.key());
			if (matcher.matches()) {
				String envVar = matcher.group(1);	
				if (config.hasValue()) {
					// simple version
					processAsSimple(envVar, config, out);
				} else {
					// when/otherwise version
					processAsWhenOtherwise(envVar, config, out);
				}
			} else {
				out.add(config);
			}
		} else {
			out.add(config);
			return;
		}	
	}
	
	private static void processAsSimple(String envVar, Config config, Output out) {
		checkConfig(config.hasValue(), "must have value");
		if (matchVar(envVar, config.valueAsString())) {
			out.add(config.body());
		}
	}
	
	private static void processAsWhenOtherwise(String envVar, Config config, Output out) {
		
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
				if (matchVar(envVar, c.getKey())) {
					out.add(c.getValue());
					caseMatch = true;
					break;
				}
			}
			
			if (!caseMatch) {
				for (ConfigBody e : otherwises) {
					out.add(e);
				}
			}
		}
	}
	
	private static boolean matchVar(String envVar, String vals) {
		Map<String, String> env = System.getenv();
		String envVal = env.get(envVar);
		for (String val : splitter.split(vals)) {
			if (val.startsWith("!")) {
				val = val.substring(1);
				if (!val.equals(envVal)) return true;
			} else {
				if (val.equals(envVal)) return true;
			}
		}
		return false;
	}

}
