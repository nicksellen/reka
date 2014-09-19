package reka.http;

import static reka.api.Path.path;
import static reka.api.Path.slashes;
import reka.core.bundle.BundleConfigurer;
import reka.http.server.HttpServerManager;

public class HttpBundle implements BundleConfigurer {
	
	private final HttpServerManager server = new HttpServerManager();
	
	@Override
	public void setup(BundleSetup bundle) {
		bundle.module(path("http"), () -> new HttpModule(server));
		bundle.module(slashes("http/sessions"), () -> new HttpSessionsModule());
		bundle.module(path("websockets"), () -> new WebsocketModule(server));
		//setup.use(slashes("admin/http"), () -> new HttpAdminModule(server));
		
		bundle.shutdown(() -> server.shutdown());
	}

}
