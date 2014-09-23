package reka.core.bundle;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import reka.api.Path;
import reka.config.processor.ConfigConverter;
import reka.core.setup.ModuleConfigurer;

public interface BundleConfigurer {
	
	void setup(BundleSetup bundle);
	
	public static class ModuleInfo implements Supplier<ModuleConfigurer> {
		
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
	
	public static class BundleSetup {
		
		private final List<ModuleInfo> modules = new ArrayList<>();
		private final List<ConfigConverter> converters = new ArrayList<>();
		private final List<BundleConfigurer> moreBundles = new ArrayList<>();
		private final List<Runnable> shutdownHandlers = new ArrayList<>();
		
		public BundleSetup module(Path name, String version, Supplier<ModuleConfigurer> supplier) {
			modules.add(new ModuleInfo(name, version, supplier));
			return this;
		}
		
		public BundleSetup converter(ConfigConverter converter) {
			converters.add(converter);
			return this;
		}
		
		public BundleSetup bundle(BundleConfigurer bundle) {
			moreBundles.add(bundle);
			return this;
		}
		
		public BundleSetup bundles(BundleConfigurer... bundles) {
			for (BundleConfigurer bundle : bundles) {
				moreBundles.add(bundle);
			}
			return this;
		}

		public void shutdown(Runnable handler) {
			shutdownHandlers.add(handler);
		}
		
		protected List<ModuleInfo> modules() {
			return modules;
		}
		
		protected List<ConfigConverter> converters() {
			return converters;
		}
		
		protected List<BundleConfigurer> moreBundles() {
			return moreBundles;
		}
		
		protected List<Runnable> shutdownHandlers() {
			return shutdownHandlers;
		}
	
	}

}
