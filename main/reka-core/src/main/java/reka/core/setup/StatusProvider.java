package reka.core.setup;

import reka.api.data.Data;
import reka.api.data.MutableData;
import reka.core.data.memory.MutableMemoryData;


public interface StatusProvider extends StatusDataProvider {
	
	public static class StatusReport {
		
		private final String name;
		private final boolean up;
		private final Data data;
		
		public StatusReport(String name, boolean up, Data data) {
			this.name = name;
			this.up = up;
			this.data = data;
		}
		
		public String name() {
			return name;
		}
		
		public boolean up() {
			return up;
		}
		
		public Data data() {
			return data;
		}
		
	}
	
	static StatusProvider create(String name, StatusDataProvider data) {
		return new DefaultStatusProvider(name, data);
	}
	
	static class DefaultStatusProvider implements StatusProvider {
		
		private final String name;
		private final StatusDataProvider provider;
		
		private DefaultStatusProvider(String name, StatusDataProvider data) {
			this.name = name;
			this.provider = data;	
		}

		@Override
		public String name() {
			return name;
		}

		@Override
		public boolean up() {
			return provider.up();
		}

		@Override
		public void statusData(MutableData data) {
			provider.statusData(data);
		}
		
	}
	
	String name();
	
	default StatusReport report() {
		MutableData data = MutableMemoryData.create();
		statusData(data);
		return new StatusReport(name(), up(), data);
	}
	
}
