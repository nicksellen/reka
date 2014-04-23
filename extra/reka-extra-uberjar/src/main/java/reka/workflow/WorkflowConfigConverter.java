package reka.workflow;

import static java.lang.String.format;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import reka.config.Config;
import reka.config.ConfigUtil;
import reka.config.processor.ConfigConverter;

public class WorkflowConfigConverter implements ConfigConverter {
	
	private String workflowFlowName(String name) {
		return format("resume [%s]", name);
	}
	
	@Override
	public void convert(Config config, Output out) {
		
		if (config.hasKey() && "workflow/resume".equals(config.key())) {
			out.keyvalue("run", workflowFlowName(config.hasValue() ? config.valueAsString() : "default"));
		} else if (config.hasBody()) {
			String workflowName = null;
			List<Config> before = new ArrayList<>();
			List<Config> currentSection = before;

			Map<String,List<Config>> afterPause = new HashMap<>();
			
			for (Config child : config.body()) {
				if (child.hasKey() && child.key().equals("workflow/pause")) {
					workflowName = child.hasValue() ? child.valueAsString() : "default";
					currentSection = new ArrayList<>();
					currentSection.add(ConfigUtil.kv(config.source(), "workflow/load", workflowName));
					afterPause.put(workflowName, currentSection);
					
					before.add(ConfigUtil.kv(config.source(), "workflow/save", workflowName));
					
					if (child.hasBody()) {
						for (Config c : child.body()) {
							before.add(c);
						}
					}
					
				} else {
					currentSection.add(child);
				}
			}
			
			if (workflowName != null) {
				
				for (Entry<String, List<Config>> e : afterPause.entrySet()) {
					out.toplevel().obj("run", workflowFlowName(e.getKey()), e.getValue());
				}
			}

			out.obj(config.key(), config.value(), before);
			
		} else {

			out.add(config); // leave it as is
			
			/*
		
			if (config.hasKey() && config.key().equals("workflow/pause")) {
				String val = config.hasValue() ? config.valueAsString() : "default";
				if (!workflowAfterPause.containsKey(val)) {
					out.add(config); // we haven't processed it yet, but we need to do it from one level up...
				}
			} else {
				out.add(config); // leave it as is
			}
			*/
		}
	}

	@Override
	public ConfigConverter resetOrClone() {
		return new WorkflowConfigConverter();
	}

}
