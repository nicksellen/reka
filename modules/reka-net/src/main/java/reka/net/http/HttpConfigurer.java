package reka.net.http;

import static reka.api.Path.path;
import static reka.config.configurer.Configurer.configure;
import static reka.config.configurer.Configurer.Preconditions.checkConfig;
import static reka.util.Util.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import reka.config.Config;
import reka.config.configurer.annotations.Conf;
import reka.core.config.ConfigurerProvider;
import reka.core.config.SequenceConfigurer;
import reka.core.module.ModuleInfo;
import reka.core.setup.AppSetup;
import reka.core.setup.ModuleConfigurer;
import reka.core.setup.OperationConfigurer;
import reka.net.NetManager;
import reka.net.NetManager.HttpFlows;
import reka.net.NetSettings;
import reka.net.NetSettings.SslSettings;
import reka.net.NetSettings.Type;
import reka.net.common.sockets.NetStatusProvider;
import reka.net.http.configurers.HttpContentConfigurer;
import reka.net.http.configurers.HttpRedirectConfigurer;
import reka.net.http.configurers.HttpRequestConfigurer;
import reka.net.http.configurers.HttpRouterConfigurer;
import reka.net.http.operations.BasicAuthConfigurer;
import reka.net.http.streaming.HttpEndConfigurer;
import reka.net.http.streaming.HttpHeadConfigurer;
import reka.net.http.streaming.HttpWriteConfigurer;

public class HttpConfigurer extends ModuleConfigurer {
	
	// listen 8080
	// listen localhost:500
	// listen boo.com
	
	private final Pattern listenPortOnly = Pattern.compile("^[0-9]+$");
	private final Pattern listenHostAndPort = Pattern.compile("^(.+):([0-9]+)$");
	
	private SslSettings ssl;
	
	private final NetManager net;
	
	private final List<HostAndPort> listens = new ArrayList<>();
	private final List<Function<ConfigurerProvider,OperationConfigurer>> requestHandlers = new ArrayList<>();
	
	public HttpConfigurer(NetManager net) {
		this.net = net;
	}
	
	@Conf.Each("listen")
	public void listen(String val) {
		String host = null;
		int port = -1; // will use default for protocol later
		if (listenPortOnly.matcher(val).matches()) {
			port = Integer.valueOf(val);
		} else {
			Matcher m = listenHostAndPort.matcher(val);
			if (m.matches()) {
				host = m.group(1);
				port = Integer.valueOf(m.group(2));
			} else {
				host = val;
			}
		}
		listens.add(new HostAndPort(host, port));
	}
	
	@Conf.At("ssl")
	public void ssl(Config config) {
		ssl = configure(new SslConfigurer(), config).build();
	}

	@Conf.Each("on")
	public void on(Config config) {
		checkConfig(config.hasValue(), "must have a value");
		switch (config.valueAsString()) {
		case "request":
			requestHandlers.add(provider -> configure(new SequenceConfigurer(provider), config.body()));
			break;
		default:
			throw runtime("unknown trigger %s", config.valueAsString());
		}
	}
	
	@Conf.At("sessions")
	public void sessions(Config config) {
		configureModule(new ModuleInfo(fullAliasOrName().add("session"), info().version(), () -> new HttpSessionsConfigurer()), config);
	}

	@Override
	public void setup(AppSetup app) {
		
		listens.replaceAll(listen -> listen.port() == -1 ? new HostAndPort(listen.host(), ssl != null ? 443 : 80) : listen);
		
		app.defineOperation(path("router"), provider -> new HttpRouterConfigurer(dirs(), provider));
		app.defineOperation(path("redirect"), provider -> new HttpRedirectConfigurer());
		app.defineOperation(path("content"), provider -> new HttpContentConfigurer(dirs()));
		app.defineOperation(path("request"), provider -> new HttpRequestConfigurer(net.nettyEventGroup(), net.nettyChannelType()));
		app.defineOperation(path("req"), provider -> new HttpRequestConfigurer(net.nettyEventGroup(), net.nettyChannelType()));
		app.defineOperation(path("auth"), provider -> new BasicAuthConfigurer(provider));
		
		// ones that take care of writing responses
		
		app.defineOperation(path("head"), provider -> new HttpHeadConfigurer());
		app.defineOperation(path("write"), provider -> new HttpWriteConfigurer());
		app.defineOperation(path("end"), provider -> new HttpEndConfigurer());
		
		/*
		 * TODO: make this, I need a nice api for provider.add(path("head"), new HttpHeadConfigurer() etc
		//module.operation(path("streaming"), provider -> provider.add)
		 
		module.operation(path("streaming"), provider -> {
			provider = provider.add(path("head"), provider -> new HttpHeadConfigurer());
			provider = provider.add(path("write"), provider -> new HttpWriteConfigurer());
			return new SequenceConfigurer(provider);
		});
		*/
		
		app.registerStatusProvider(() -> new NetStatusProvider(net, app.identity(), NetSettings.Type.HTTP));
		
		listens.forEach(listen -> {
			app.requireNetwork(listen.port(), listen.host());	
		});
		
		for (Function<ConfigurerProvider, OperationConfigurer> h : requestHandlers) {
			
			app.buildFlow("on request", h, flow -> {
				
				for (HostAndPort listen : listens) {
					
					if (ssl != null) {
						app.registerComponent(net.deployHttps(app.identity(), listen, ssl, new HttpFlows(flow)));
					} else {
						app.registerComponent(net.deployHttp(app.identity(), listen, new HttpFlows(flow)));
					}
					
					app.registerNetwork(listen.port(), Type.HTTP.protocolString(ssl != null), details -> {
						details.putString("host", listen.host());
					});
				
				}
			});
		}
		
	}

}
