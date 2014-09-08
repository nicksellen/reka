package reka.http;

import static reka.api.Path.path;
import static reka.api.Path.slashes;
import reka.core.bundle.RekaBundle;
import reka.http.server.HttpServerManager;

public class HttpBundle implements RekaBundle {
	
	private final HttpServerManager server = new HttpServerManager();
	
	@Override
	public void setup(BundleSetup bundle) {
		bundle.module(path("http"), () -> new HttpModule(server, false));
		bundle.module(path("https"), () -> new HttpModule(server, true));
		bundle.module(slashes("http/sessions"), () -> new HttpSessionsModule());
		bundle.module(path("websockets"), () -> new WebsocketModule(server, false));
		bundle.module(path("websockets-ssl"), () -> new WebsocketModule(server, true));
		//setup.use(slashes("admin/http"), () -> new HttpAdminModule(server));
	}

}
