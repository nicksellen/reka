package reka.http;

import static reka.api.Path.path;
import reka.api.Path;
import reka.core.module.Module;
import reka.core.module.ModuleDefinition;
import reka.http.server.HttpServerManager;

public class HttpModule implements Module {

	@Override
	public Path base() {
		return path("http");
	}
	
	private final HttpServerManager server = new HttpServerManager();
	
	@Override
	public void setup(ModuleDefinition module) {
		module.main(() -> new HttpConfigurer(server));
		module.submodule(path("sessions"), () -> new HttpSessionsConfigurer());
		module.submodule(path("websockets"), () -> new WebsocketConfigurer(server));
		module.onShutdown(() -> server.shutdown());
	}

}
