package reka.config.parser.values;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import reka.config.Config;

public class BodyVal {
	
	private final List<Config> configs;
	
	public BodyVal(Collection<Config> configs) {
		this.configs = new ArrayList<>(configs);
	}
	
	public List<Config> configs() {
		return configs;
	}
	
}