package reka.net.http;

import static java.lang.String.format;
import static reka.api.Path.path;
import static reka.config.configurer.Configurer.configure;
import static reka.config.configurer.Configurer.Preconditions.checkConfig;
import static reka.util.Util.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import reka.config.Config;
import reka.config.configurer.annotations.Conf;
import reka.core.config.ConfigurerProvider;
import reka.core.config.SequenceConfigurer;
import reka.core.module.ModuleInfo;
import reka.core.setup.ModuleConfigurer;
import reka.core.setup.ModuleSetup;
import reka.core.setup.OperationConfigurer;
import reka.net.NetServerManager;
import reka.net.NetServerManager.HttpFlows;
import reka.net.NetSettings;
import reka.net.NetSettings.SslSettings;
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
	
	private final NetServerManager server;
	
	private final List<HostAndPort> listens = new ArrayList<>();
	private final List<Function<ConfigurerProvider,OperationConfigurer>> requestHandlers = new ArrayList<>();
	
	public HttpConfigurer(NetServerManager server) {
		this.server = server;
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
	public void setup(ModuleSetup module) {
		
		listens.replaceAll(listen -> listen.port() == -1 ? new HostAndPort(listen.host(), ssl != null ? 443 : 80) : listen);
		
		module.operation(path("router"), provider -> new HttpRouterConfigurer(dirs(), provider));
		module.operation(path("redirect"), provider -> new HttpRedirectConfigurer());
		module.operation(path("content"), provider -> new HttpContentConfigurer(dirs()));
		module.operation(path("request"), provider -> new HttpRequestConfigurer(server.nettyEventGroup(), server.nettyChannelType()));
		module.operation(path("req"), provider -> new HttpRequestConfigurer(server.nettyEventGroup(), server.nettyChannelType()));
		module.operation(path("auth"), provider -> new BasicAuthConfigurer(provider));
		
		// ones that take care of writing responses
		
		module.operation(path("head"), provider -> new HttpHeadConfigurer());
		module.operation(path("write"), provider -> new HttpWriteConfigurer());
		module.operation(path("end"), provider -> new HttpEndConfigurer());
		
		/*
		 * TODO: make this, I need a nice api for provider.add(path("head"), new HttpHeadConfigurer() etc
		//module.operation(path("streaming"), provider -> provider.add)
		 
		module.operation(path("streaming"), provider -> {
			provider = provider.add(path("head"), provider -> new HttpHeadConfigurer());
			provider = provider.add(path("write"), provider -> new HttpWriteConfigurer());
			return new SequenceConfigurer(provider);
		});
		*/
		
		listens.forEach(listen -> {
			module.requirePort(listen.port(), Optional.of(listen.host()));	
		});
		
		for (Function<ConfigurerProvider, OperationConfigurer> h : requestHandlers) {
			
			module.trigger("on request", h, reg -> {
				
				for (HostAndPort listen : listens) {
					
					String id = format("%s/%s/%s/http", reg.applicationIdentity(), listen.host(), listen.port());
					NetSettings settings = httpSettings(listen, reg.applicationIdentity(), reg.applicationVersion());
					
					server.deployHttp(id, settings, new HttpFlows(reg.flow()));
					
					reg.onUndeploy(version -> server.undeploy(id, version));
					reg.onPause(version -> server.pause(id, version));
					reg.onResume(version -> server.resume(id, version));
					
					reg.network(listen.port(), settings.protocolString(), details -> {
						details.putString("host", listen.host());
					});
				
				}
			});
		}
		
	}
	
	private NetSettings httpSettings(HostAndPort listen, String applicationIdentity, int applicationVersion) {
		String host = listen.host();
		int port = listen.port();
		
		if (port == -1) {
			port = ssl != null ? 443 : 80;
		}
	
		if (ssl != null) {
			return NetSettings.https(port, host, applicationIdentity, applicationVersion, ssl);
		} else {
			return NetSettings.http(port, host, applicationIdentity, applicationVersion);
		}
	}

}
