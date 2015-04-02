package reka.net;

import static reka.api.Path.path;
import reka.core.setup.ModuleConfigurer;
import reka.core.setup.AppSetup;
import reka.net.http.configurers.NetProxyConfigurer;

public class NetConfigurer extends ModuleConfigurer {

	private final NetManager server;
	
	public NetConfigurer(NetManager server) {
		this.server = server;
	}

	@Override
	public void setup(AppSetup module) {
		module.defineOperation(path("proxy"), provider -> new NetProxyConfigurer(server.nettyEventGroup()));
	}	
	
}
