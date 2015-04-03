package reka.net;

import static reka.util.Path.path;
import static reka.util.Path.slashes;
import io.netty.channel.Channel;

import javax.inject.Singleton;

import reka.identity.IdentityKey;
import reka.module.Module;
import reka.module.ModuleDefinition;
import reka.net.http.HttpConfigurer;
import reka.net.http.HttpSessionsConfigurer;
import reka.net.socket.SocketConfigurer;
import reka.net.websockets.WebsocketConfigurer;
import reka.util.Path;

@Singleton
public class NetModule implements Module {
	
	public static interface Keys {
		static final IdentityKey<Channel> channel = IdentityKey.named("netty channel");
	}

	private final NetManager server = new NetManager();

	@Override
	public Path base() {
		return path("net");
	}
	
	@Override
	public void setup(ModuleDefinition module) {
		
		module.registerPortChecker(server.portChecker);
		
		module.main(() -> new NetConfigurer(server));
		
		module.submodule(slashes("http"), () -> new HttpConfigurer(server));
		module.submodule(slashes("http/sessions"), () -> new HttpSessionsConfigurer());
		module.submodule(slashes("websockets"), () -> new WebsocketConfigurer(server));
		module.submodule(slashes("socket"), () -> new SocketConfigurer(server));
		module.onShutdown(server::shutdown);
		
	}

	public NetManager server() {
		return server;
	}
	
}
