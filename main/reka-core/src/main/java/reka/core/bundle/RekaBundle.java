package reka.core.bundle;

import static reka.util.Util.createEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.function.Supplier;

import reka.api.Path;
import reka.config.processor.ConfigConverter;
import reka.core.setup.ModuleConfigurer;

public interface RekaBundle {
	
	void setup(BundleSetup bundle);
	
	public static class BundleSetup {
		
		private final List<Entry<Path,Supplier<ModuleConfigurer>>> uses = new ArrayList<>();
		private final List<ConfigConverter> converters = new ArrayList<>();
		private final List<RekaBundle> moreBundles = new ArrayList<>();
		private final List<Runnable> shutdownHandlers = new ArrayList<>();
		
		public BundleSetup module(Path name, Supplier<ModuleConfigurer> supplier) {
			uses.add(createEntry(name, supplier));
			return this;
		}
		
		public BundleSetup converter(ConfigConverter converter) {
			converters.add(converter);
			return this;
		}
		
		public BundleSetup bundle(RekaBundle bundle) {
			moreBundles.add(bundle);
			return this;
		}
		
		public BundleSetup bundles(RekaBundle... bundles) {
			for (RekaBundle bundle : bundles) {
				moreBundles.add(bundle);
			}
			return this;
		}

		public void shutdown(Runnable handler) {
			shutdownHandlers.add(handler);
		}
		
		protected List<Entry<Path,Supplier<ModuleConfigurer>>> modules() {
			return uses;
		}
		
		protected List<ConfigConverter> converters() {
			return converters;
		}
		
		protected List<RekaBundle> moreBundles() {
			return moreBundles;
		}
		
		protected List<Runnable> shutdownHandlers() {
			return shutdownHandlers;
		}
	
	}

}
