package reka.module.setup;

import java.util.HashMap;

import reka.data.MutableData;
import reka.data.memory.MutableMemoryData;


public interface StatusProvider extends StatusDataProvider {
	
	static StatusProvider create(String name, String alias, String version) {
		return create(name, alias, version, null);
	}
	
	static StatusProvider create(String name, String alias, String version, StatusDataProvider provider) {
		return new DefaultStatusProvider(name, alias, version, provider);
	}
	
	static class DefaultStatusProvider implements StatusProvider {
		
		private final String name;
		private final String alias;
		private final String version;
		private final StatusDataProvider provider;
		
		private DefaultStatusProvider(String name, String alias, String version, StatusDataProvider provider) {
			this.name = name;
			this.alias = alias;
			this.version = version;
			this.provider = provider;
		}

		@Override
		public String name() {
			return name;
		}
		
		@Override
		public String alias() {
			return alias;
		}
		
		@Override
		public String version() {
			return version;
		}

		@Override
		public boolean up() {
			return provider != null ? provider.up() : true;
		}

		@Override
		public void statusData(MutableData data) {
			if (provider != null) provider.statusData(data);
		}
		
	}
	
	String name();
	String alias();
	String version();
	
	default ModuleStatusReport report() {
		MutableData data = MutableMemoryData.createFromMap(new HashMap<>());
		statusData(data);
		return new ModuleStatusReport(name(), alias(), version(), up(), data);
	}
	
}
