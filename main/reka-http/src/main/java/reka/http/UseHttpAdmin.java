package reka.http;

import static reka.api.Path.dots;
import static reka.core.builder.FlowSegments.sync;

import java.util.Map.Entry;
import java.util.function.Supplier;

import reka.api.Path;
import reka.api.data.MutableData;
import reka.api.flow.FlowSegment;
import reka.api.run.SyncOperation;
import reka.core.bundle.UseConfigurer;
import reka.core.bundle.UseInit;
import reka.http.server.HttpServer;
import reka.http.server.HttpSettings;

public class UseHttpAdmin extends UseConfigurer {

	private final HttpServer server;
	
	public UseHttpAdmin(HttpServer server) {
		this.server = server;
	}
	
	@Override
	public void setup(UseInit use) {
		use.operation("list", () -> new ListConfigurer());
	}
	
	class ListConfigurer implements Supplier<FlowSegment> {
		
		private Path out = dots("admin.http.deployed");

		@Override
		public FlowSegment get() {
			return sync("list", () -> new ListOperation(out));
		}
		
	}
	
	class ListOperation implements SyncOperation {
		
		private final Path out;
		
		ListOperation(Path out) {
			this.out = out;
		}

		@Override
		public MutableData call(MutableData data) {
			for (Entry<String, HttpSettings> e : server.deployed().entrySet()) {
				MutableData entry = data.createMapAt(out.add(e.getKey()));
				HttpSettings settings = e.getValue();
				entry.putString("host", settings.host());
				entry.putInt("port", settings.port());
			}
			return data;
		}
		
	}
 
}
