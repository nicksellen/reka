package reka.net;

import static reka.api.Path.path;
import reka.core.setup.ModuleConfigurer;
import reka.core.setup.ModuleSetup;
import reka.net.http.configurers.NetProxyConfigurer;

public class NetConfigurer extends ModuleConfigurer {

	private final NetServerManager server;
	
	public NetConfigurer(NetServerManager server) {
		this.server = server;
	}

	@Override
	public void setup(ModuleSetup module) {
		module.defineOperation(path("proxy"), provider -> new NetProxyConfigurer(server.nettyEventGroup()));
	}	
	
}
