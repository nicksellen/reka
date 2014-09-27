package reka;

import reka.core.module.Module;

public class ModuleMeta {
	
	private final String version;
	private final Module module;

	public ModuleMeta(String version, Module module) {
		this.version = version;
		this.module = module;
	}

	public String version() {
		return version;
	}
	
	public Module module() {
		return module;
	}

}
