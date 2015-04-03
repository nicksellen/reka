package reka.module;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import reka.config.processor.ConfigConverter;
import reka.config.processor.MultiConverter;
import reka.config.processor.Processor;
import reka.util.AsyncShutdown;
import reka.util.Path;


public class ModuleManager implements AsyncShutdown {
	
	private final Set<ModuleMeta> modules = new HashSet<>();
	
	private final List<ModuleInfo> moduleInfos = new ArrayList<>();
	private final List<ConfigConverter> converters = new ArrayList<>();
	private final List<AsyncShutdown> shutdownHandlers = new ArrayList<>();
	private final Set<PortChecker> portCheckers = new HashSet<>();
	
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
		portCheckers.addAll(setup.portCheckers());
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
	
	public Collection<PortChecker> portCheckers() {
		return portCheckers;
	}
	
	@Override
	public void shutdown(AsyncShutdown.Result res) {
		AsyncShutdown.shutdownAll(shutdownHandlers, res);
	}
}