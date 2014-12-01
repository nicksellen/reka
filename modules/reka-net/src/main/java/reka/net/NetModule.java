package reka.net;
import static reka.api.Path.path;
import static reka.api.Path.slashes;
import reka.api.Path;
import reka.core.module.Module;
import reka.core.module.ModuleDefinition;
import reka.net.http.HttpConfigurer;
import reka.net.http.HttpSessionsConfigurer;
import reka.net.socket.SocketConfigurer;
import reka.net.websockets.WebsocketConfigurer;


public class NetModule implements Module {

	@Override
	public Path base() {
		return path("net");
	}

	private final NetServerManager server = new NetServerManager();
	
	@Override
	public void setup(ModuleDefinition module) {
		module.submodule(slashes("http"), () -> new HttpConfigurer(server));
		module.submodule(slashes("http/sessions"), () -> new HttpSessionsConfigurer());
		module.submodule(slashes("websockets"), () -> new WebsocketConfigurer(server));
		module.submodule(slashes("socket"), () -> new SocketConfigurer(server));
		module.onShutdown(() -> server.shutdown());
	}
	
}
