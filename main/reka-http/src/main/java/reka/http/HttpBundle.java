package reka.http;

import static reka.api.Path.path;
import static reka.api.Path.slashes;
import reka.core.bundle.RekaBundle;
import reka.http.server.HttpServer;

public class HttpBundle implements RekaBundle {
	
	private final HttpServer server = new HttpServer();
	
	@Override
	public void setup(Setup setup) {
		setup.use(path("http"), () -> new UseHTTP(server));
		setup.use(path("websockets"), () -> new UseWebsockets(server));
		setup.use(slashes("admin/http"), () -> new UseHttpAdmin(server));
	}

}
