package reka.core.setup;

import reka.api.data.MutableData;
import reka.core.data.memory.MutableMemoryData;


public interface StatusProvider extends StatusDataProvider {
	
	static StatusProvider create(String type, String name, String version) {
		return create(type, name, version, null);
	}
	
	static StatusProvider create(String type, String name, String version, StatusDataProvider provider) {
		return new DefaultStatusProvider(type, name, version, provider);
	}
	
	static class DefaultStatusProvider implements StatusProvider {
		
		private final String type;
		private final String name;
		private final String version;
		private final StatusDataProvider provider;
		
		private DefaultStatusProvider(String type, String name, String version, StatusDataProvider provider) {
			this.type = type;
			this.name = name;
			this.version = version;
			this.provider = provider;
		}

		@Override
		public String type() {
			return type;
		}
		
		@Override
		public String name() {
			return name;
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
	
	String type();
	String name();
	String version();
	
	default StatusReport report() {
		MutableData data = MutableMemoryData.create();
		statusData(data);
		return new StatusReport(type(), name(), version(), up(), data);
	}
	
}
