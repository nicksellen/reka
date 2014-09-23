package reka.core.bundle;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import reka.api.Path;
import reka.config.processor.ConfigConverter;
import reka.config.processor.MultiConverter;
import reka.config.processor.Processor;
import reka.core.bundle.BundleConfigurer.ModuleInfo;

public class BundleManager {
	
	private final Set<BundleConfigurer> bundles = new HashSet<>();
	
	private final List<ModuleInfo> modules = new ArrayList<>();
	private final List<ConfigConverter> converters = new ArrayList<>();
	private final List<Runnable> shutdownHandlers = new ArrayList<>();
	
	public BundleManager(Collection<BundleConfigurer> incoming) {
		
		bundles.addAll(incoming);
		
		for (BundleConfigurer bundle : incoming) {
			setupBundle(bundle);
		}
		
	}
	
	public BundleManager add(BundleConfigurer bundle) {
		if (bundles.add(bundle)) {
			setupBundle(bundle);
		}
		return this;
	}
	
	private void setupBundle(BundleConfigurer bundle) {
		BundleConfigurer.BundleSetup setup = new BundleConfigurer.BundleSetup();
		bundle.setup(setup);
		modules.addAll(setup.modules());
		converters.addAll(setup.converters());
		for (BundleConfigurer extraBundle : setup.moreBundles()) {
			add(extraBundle);
		}
		shutdownHandlers.addAll(setup.shutdownHandlers());
	}
	
	public List<ModuleInfo> modules() {
		return modules;
	}
	
	public Processor processor() {
		return new Processor(new MultiConverter(converters));
	}
	
	public Collection<Path> modulesKeys() {
		Set<Path> keys = new HashSet<>();
		for (ModuleInfo e : modules) {
			keys.add(e.name());
		}
		return keys; 
	}
	
	public void shutdown() {
		shutdownHandlers.forEach(handler -> handler.run());
	}

}