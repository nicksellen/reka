package reka.core.module;

import static reka.api.Path.root;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import reka.api.Path;
import reka.config.processor.ConfigConverter;
import reka.core.setup.ModuleConfigurer;

public class ModuleDefinition {
	
	private final Path base;
	private final String version;
	
	private final List<ModuleInfo> modules = new ArrayList<>();
	private final List<ConfigConverter> converters = new ArrayList<>();
	private final List<Runnable> shutdownHandlers = new ArrayList<>();
	
	public ModuleDefinition(Path base, String version) {
		this.base = base;
		this.version = version;
	}
	
	public ModuleDefinition main(Supplier<ModuleConfigurer> supplier) {
		return submodule(root(), supplier);
	}
	
	public ModuleDefinition submodule(Path name, Supplier<ModuleConfigurer> supplier) {
		modules.add(new ModuleInfo(base.add(name), version, supplier));
		return this;
	}
	
	public ModuleDefinition converter(ConfigConverter converter) {
		converters.add(converter);
		return this;
	}

	public void onShutdown(Runnable handler) {
		shutdownHandlers.add(handler);
	}
	
	protected List<ModuleInfo> modules() {
		return modules;
	}
	
	protected List<ConfigConverter> converters() {
		return converters;
	}
	
	protected List<Runnable> shutdownHandlers() {
		return shutdownHandlers;
	}

}