package reka.core.module;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import reka.ModuleMeta;
import reka.api.Path;
import reka.config.processor.ConfigConverter;
import reka.config.processor.MultiConverter;
import reka.config.processor.Processor;

public class ModuleManager {
	
	private final Set<ModuleMeta> modules = new HashSet<>();
	
	private final List<ModuleInfo> moduleInfos = new ArrayList<>();
	private final List<ConfigConverter> converters = new ArrayList<>();
	private final List<Runnable> shutdownHandlers = new ArrayList<>();
	
	public ModuleManager(Collection<ModuleMeta> incoming) {
		
		modules.addAll(incoming);
		
		for (ModuleMeta module : incoming) {
			setupModule(module);
		}
		
	}
	
	public ModuleManager add(ModuleMeta module) {
		if (modules.add(module)) {
			setupModule(module);
		}
		return this;
	}
	
	private void setupModule(ModuleMeta module) {
		ModuleDefinition setup = new ModuleDefinition(module.module().base(), module.version());
		module.module().setup(setup);
		moduleInfos.addAll(setup.modules());
		converters.addAll(setup.converters());
		shutdownHandlers.addAll(setup.shutdownHandlers());
	}
	
	public List<ModuleInfo> modules() {
		return moduleInfos;
	}
	
	public Processor processor() {
		return new Processor(new MultiConverter(converters));
	}
	
	public Collection<Path> modulesKeys() {
		Set<Path> keys = new HashSet<>();
		for (ModuleInfo e : moduleInfos) {
			keys.add(e.name());
		}
		return keys; 
	}
	
	public void shutdown() {
		shutdownHandlers.forEach(handler -> handler.run());
	}

}