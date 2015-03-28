package io.reka.usenetplay;

import reka.Identity;
import reka.config.Config;
import reka.config.ConfigBody;
import reka.config.configurer.annotations.Conf;
import reka.core.setup.ModuleConfigurer;
import reka.core.setup.ModuleSetup;
import reka.net.NetServerManager;
import reka.net.NetServerManager.HttpFlows;
import reka.net.http.HostAndPort;

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
	public void setup(ModuleSetup app) {
		
		app.buildFlow("on addy req", body, flow -> {
			
			int port = 9090;
			
			app.registerComponent(server.deployHttp(app.identity(), new HostAndPort(addy, port), new HttpFlows(flow)));
			
			app.registerNetwork(port, "http", details -> {
				details.putString("host", addy);
			});
			
		});
	}
	
}
