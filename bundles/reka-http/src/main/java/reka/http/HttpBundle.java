package reka.http;

import static reka.api.Path.path;
import reka.api.Path;
import reka.core.bundle.BundleConfigurer;
import reka.http.server.HttpServerManager;

public class HttpBundle implements BundleConfigurer {

	@Override
	public Path base() {
		return path("http");
	}
	
	private final HttpServerManager server = new HttpServerManager();
	
	@Override
	public void setup(BundleSetup bundle) {
		bundle.module("0.1.0", () -> new HttpModule(server));
		bundle.submodule(path("sessions"), "0.1.0", () -> new HttpSessionsModule());
		bundle.submodule(path("websockets"), "0.1.0", () -> new WebsocketModule(server));
		bundle.shutdown(() -> server.shutdown());
	}

}
