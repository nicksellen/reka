package reka.module;

import java.util.function.Supplier;

import reka.module.setup.ModuleConfigurer;
import reka.util.Path;

public class ModuleInfo implements Supplier<ModuleConfigurer> {
	
	private final Path name;
	private final String version;
	private final Supplier<ModuleConfigurer> supplier;
	
	public ModuleInfo(Path name, String version, Supplier<ModuleConfigurer> supplier) {
		this.name = name;
		this.version = version;
		this.supplier = supplier;
	}
	
	public Path name() {
		return name;
	}
	
	public String version() {
		return version;
	}

	@Override
	public ModuleConfigurer get() {
		return supplier.get();
	}
	
}