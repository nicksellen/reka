package reka.http;

import static reka.api.Path.dots;
import static reka.api.Path.path;

import java.util.Map.Entry;

import reka.api.Path;
import reka.api.data.MutableData;
import reka.api.run.Operation;
import reka.core.setup.ModuleConfigurer;
import reka.core.setup.ModuleSetup;
import reka.core.setup.OperationSetup;
import reka.http.server.HttpServerManager;
import reka.http.server.HttpSettings;
import reka.nashorn.OperationConfigurer;

public class HttpAdminModule extends ModuleConfigurer {

	private final HttpServerManager server;
	
	public HttpAdminModule(HttpServerManager server) {
		this.server = server;
	}
	
	@Override
	public void setup(ModuleSetup use) {
		use.operation(path("list"), provider -> new ListConfigurer());
	}
	
	class ListConfigurer implements OperationConfigurer {
		
		private Path out = dots("admin.http.deployed");

		@Override
		public void setup(OperationSetup ops) {
			ops.add("list", store -> new ListOperation(out));
		}
		
	}
	
	class ListOperation implements Operation {
		
		private final Path out;
		
		ListOperation(Path out) {
			this.out = out;
		}

		@Override
		public void call(MutableData data) {
			for (Entry<String, HttpSettings> e : server.deployed().entrySet()) {
				MutableData entry = data.createMapAt(out.add(e.getKey()));
				HttpSettings settings = e.getValue();
				entry.putString("host", settings.host());
				entry.putInt("port", settings.port());
			}
		}
		
	}
 
}
