package reka.config.parser.values;

import java.util.List;

import reka.config.Config;

import com.google.common.collect.ImmutableList;

public class BodyVal {
	
	private final List<Config> configs;
	
	public BodyVal(List<Config> configs) {
		this.configs = ImmutableList.copyOf(configs);
	}
	
	public List<Config> configs() {
		return configs;
	}
	
}