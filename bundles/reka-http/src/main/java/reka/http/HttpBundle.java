package reka.http;

import static reka.api.Path.path;
import static reka.api.Path.slashes;
import reka.core.bundle.RekaBundle;
import reka.http.server.HttpServerManager;

public class HttpBundle implements RekaBundle {
	
	private final HttpServerManager server = new HttpServerManager();
	
	@Override
	public void setup(BundleSetup setup) {
		setup.use(path("http"), () -> new HttpModule(server));
		setup.use(path("websockets"), () -> new WebsocketsModule(server));
		setup.use(slashes("admin/http"), () -> new HttpAdminModule(server));
	}

}
