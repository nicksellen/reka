package io.reka.usenetplay;

import reka.config.Config;
import reka.config.ConfigBody;
import reka.config.configurer.annotations.Conf;
import reka.core.setup.ModuleConfigurer;
import reka.core.setup.AppSetup;
import reka.net.NetManager;
import reka.net.NetManager.HttpFlows;
import reka.net.http.HostAndPort;

public class UseNetPlayConfigurer extends ModuleConfigurer  {
	
	private final NetManager server;
	
	private String addy;
	private ConfigBody body;

	public UseNetPlayConfigurer(NetManager server) {
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
	public void setup(AppSetup app) {
		
		app.buildFlow("on addy req", body, flow -> {
			
			int port = 9090;
			
			app.registerComponent(server.deployHttp(app.identity(), new HostAndPort(addy, port), new HttpFlows(flow)));
			
			app.registerNetwork(port, "http", details -> {
				details.putString("host", addy);
			});
			
		});
	}
	
}
