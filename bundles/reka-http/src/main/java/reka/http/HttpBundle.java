package reka.http;

import static reka.api.Path.path;
import static reka.api.Path.slashes;
import reka.core.bundle.BundleConfigurer;
import reka.http.server.HttpServerManager;

public class HttpBundle implements BundleConfigurer {
	
	private final HttpServerManager server = new HttpServerManager();
	
	@Override
	public void setup(BundleSetup bundle) {
		bundle.module(path("http"), "0.1.0", () -> new HttpModule(server));
		bundle.module(slashes("http/sessions"), "0.1.0", () -> new HttpSessionsModule());
		bundle.module(path("websockets"), "0.1.0", () -> new WebsocketModule(server));
		bundle.shutdown(() -> server.shutdown());
	}

}
