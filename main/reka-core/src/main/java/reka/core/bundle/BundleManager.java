package reka.core.bundle;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Supplier;

import reka.api.Path;
import reka.config.processor.ConfigConverter;
import reka.config.processor.MultiConverter;
import reka.config.processor.Processor;

public class BundleManager {
	
	private final Set<RekaBundle> bundles = new HashSet<>();
	
	private final List<Entry<Path,Supplier<ModuleConfigurer>>> uses = new ArrayList<>();
	private final List<ConfigConverter> converters = new ArrayList<>();
	
	public BundleManager(Collection<RekaBundle> incoming) {
		
		bundles.addAll(incoming);
		
		for (RekaBundle bundle : incoming) {
			setupBundle(bundle);
		}
		
	}
	
	public BundleManager add(RekaBundle bundle) {
		if (bundles.add(bundle)) {
			setupBundle(bundle);
		}
		return this;
	}
	
	private void setupBundle(RekaBundle bundle) {
		RekaBundle.BundleSetup setup = new RekaBundle.BundleSetup();
		bundle.setup(setup);
		uses.addAll(setup.uses());
		converters.addAll(setup.converters());
		for (RekaBundle extraBundle : setup.moreBundles()) {
			add(extraBundle);
		}
	}
	
	public List<Entry<Path,Supplier<ModuleConfigurer>>> uses() {
		return uses;
	}
	
	public Processor processor() {
		return new Processor(new MultiConverter(converters));
	}
	
	public Collection<Path> useKeys() {
		Set<Path> keys = new HashSet<>();
		for (Entry<Path, Supplier<ModuleConfigurer>> e : uses) {
			keys.add(e.getKey());
		}
		return keys; 
	}

}