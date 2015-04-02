package reka;

import reka.core.module.Module;

public class ModuleMeta {
	
	private final ClassLoader classLoader;
	private final String version;
	private final Module module;

	public ModuleMeta(ClassLoader classLoader, String version, Module module) {
		this.classLoader = classLoader;
		this.version = version;
		this.module = module;
	}
	
	public ClassLoader classLoader() {
		return classLoader;
	}

	public String version() {
		return version;
	}
	
	public Module module() {
		return module;
	}

}
