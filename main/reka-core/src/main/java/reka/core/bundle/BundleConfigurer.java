package reka.core.bundle;

import static reka.api.Path.root;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import reka.api.Path;
import reka.config.processor.ConfigConverter;
import reka.core.setup.ModuleConfigurer;

public interface BundleConfigurer {
	
	Path base();
	void setup(BundleSetup bundle);
	
	public static class ModuleInfo implements Supplier<ModuleConfigurer> {
		
		private final Path type;
		private final String version;
		private final Supplier<ModuleConfigurer> supplier;
		
		public ModuleInfo(Path type, String version, Supplier<ModuleConfigurer> supplier) {
			this.type = type;
			this.version = version;
			this.supplier = supplier;
		}
		
		public Path type() {
			return type;
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
		
		private final Path base;
		
		private final List<ModuleInfo> modules = new ArrayList<>();
		private final List<ConfigConverter> converters = new ArrayList<>();
		private final List<BundleConfigurer> moreBundles = new ArrayList<>();
		private final List<Runnable> shutdownHandlers = new ArrayList<>();
		
		public BundleSetup(Path base) {
			this.base = base;
		}
		
		public BundleSetup module(String version, Supplier<ModuleConfigurer> supplier) {
			return submodule(root(), version, supplier);
		}
		
		public BundleSetup submodule(Path name, String version, Supplier<ModuleConfigurer> supplier) {
			modules.add(new ModuleInfo(base.add(name), version, supplier));
			return this;
		}
		
		public BundleSetup converter(ConfigConverter converter) {
			converters.add(converter);
			return this;
		}
		
		/*
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
		*/

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
