package io.reka.usenetplay;

import static java.lang.String.format;
import reka.config.Config;
import reka.config.ConfigBody;
import reka.config.configurer.annotations.Conf;
import reka.core.setup.ModuleConfigurer;
import reka.core.setup.ModuleSetup;
import reka.net.NetServerManager;
import reka.net.NetSettings;

public class UseNetPlayConfigurer extends ModuleConfigurer  {
	
	private final NetServerManager server;
	
	private String addy;
	private ConfigBody body;

	public UseNetPlayConfigurer(NetServerManager server) {
		this.server = server;
	}

	@Conf.At("addy")
	public void addy(String value) {
		addy = value;
	}
	
	@Conf.At("do")
	public void conf(Config conf) {
		body = conf.body();
	}
	
	@Override
	public void setup(ModuleSetup module) {
		
		module.trigger("on addy req", body, reg -> {
			
			int port = 9090;
			
			String id = format("%s/%s/%s/http", reg.applicationIdentity(), addy, port);
			
			NetSettings settings = NetSettings.http(port, addy, reg.applicationIdentity(), reg.applicationVersion());
			
			server.deployHttp(id, reg.flow(), settings);
			
			reg.onUndeploy(version -> server.undeploy(id, version));
			reg.onPause(version -> server.pause(id, version));
			reg.onResume(version -> server.resume(id, version));
			
			reg.network(port, settings.isSsl() ? "https" : "http", details -> {
				details.putString("host", addy);
			});
			
		});
	}
	
}
