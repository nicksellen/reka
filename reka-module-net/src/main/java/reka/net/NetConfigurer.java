package reka.net;

import static reka.util.Path.path;
import reka.module.setup.AppSetup;
import reka.module.setup.ModuleConfigurer;
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
