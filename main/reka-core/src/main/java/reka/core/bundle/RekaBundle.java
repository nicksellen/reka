package reka.core.bundle;

import static reka.util.Util.createEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.function.Supplier;

import reka.api.Path;
import reka.config.processor.ConfigConverter;

public interface RekaBundle {
	
	void setup(Setup setup);
	
	public static class Setup {
		
		private final List<Entry<Path,Supplier<UseConfigurer>>> uses = new ArrayList<>();
		private final List<ConfigConverter> converters = new ArrayList<>();
		private final List<RekaBundle> moreBundles = new ArrayList<>();
		
		public Setup use(Path name, Supplier<UseConfigurer> supplier) {
			uses.add(createEntry(name, supplier));
			return this;
		}
		
		public Setup converter(ConfigConverter converter) {
			converters.add(converter);
			return this;
		}
		
		public Setup bundle(RekaBundle bundle) {
			moreBundles.add(bundle);
			return this;
		}
		
		public Setup bundles(RekaBundle... bundles) {
			for (RekaBundle bundle : bundles) {
				moreBundles.add(bundle);
			}
			return this;
		}
		
		protected List<Entry<Path,Supplier<UseConfigurer>>> uses() {
			return uses;
		}
		
		protected List<ConfigConverter> converters() {
			return converters;
		}
		
		protected List<RekaBundle> moreBundles() {
			return moreBundles;
		}
	
	}

}
