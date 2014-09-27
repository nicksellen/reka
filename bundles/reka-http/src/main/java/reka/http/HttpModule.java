package reka.http;

import static java.lang.String.format;
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
import reka.core.bundle.BundleConfigurer.ModuleInfo;
import reka.core.config.ConfigurerProvider;
import reka.core.config.SequenceConfigurer;
import reka.core.setup.ModuleConfigurer;
import reka.core.setup.ModuleSetup;
import reka.core.setup.OperationConfigurer;
import reka.http.configurers.HttpContentConfigurer;
import reka.http.configurers.HttpRedirectConfigurer;
import reka.http.configurers.HttpRequestConfigurer;
import reka.http.configurers.HttpRouterConfigurer;
import reka.http.operations.BasicAuthConfigurer;
import reka.http.server.HttpServerManager;
import reka.http.server.HttpSettings;
import reka.http.server.HttpSettings.SslSettings;
import reka.http.server.HttpSettings.Type;

public class HttpModule extends ModuleConfigurer {
	
	// listen 8080
	// listen localhost:500
	// listen boo.com
	
	private final Pattern listenPortOnly = Pattern.compile("^[0-9]+$");
	private final Pattern listenHostAndPort = Pattern.compile("^(.+):([0-9]+)$");
	
	private SslSettings ssl;
	
	private final HttpServerManager server;
	
	private final List<HostAndPort> listens = new ArrayList<>();
	private final List<Function<ConfigurerProvider,OperationConfigurer>> requestHandlers = new ArrayList<>();
	
	public HttpModule(HttpServerManager server) {
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
		configureModule(new ModuleInfo(fullPath().add("session"), info().version(), () -> new HttpSessionsModule()), config);
	}

	@Override
	public void setup(ModuleSetup module) {
		
		module.operation(path("router"), provider -> new HttpRouterConfigurer(provider));
		module.operation(path("redirect"), provider -> new HttpRedirectConfigurer());
		module.operation(path("content"), provider -> new HttpContentConfigurer());
		module.operation(path("request"), provider -> new HttpRequestConfigurer(server.nettyEventGroup(), server.nettyChannelType()));
		module.operation(path("req"), provider -> new HttpRequestConfigurer(server.nettyEventGroup(), server.nettyChannelType()));
		module.operation(path("auth"), provider -> new BasicAuthConfigurer(provider));
		
		for (Function<ConfigurerProvider, OperationConfigurer> h : requestHandlers) {
			
			module.check(check -> {
				for (HostAndPort listen : listens) {
					if (!server.isAvailable(check.applicationIdentity(), listen)) {
						check.error("%s:%s is not available", listen.host(), listen.port());
					}
				}
			});
			
			module.trigger("on request", h, registration -> {
				
				for (HostAndPort listen : listens) {

					String identity = format("%s/%s/%s/http", registration.applicationIdentity(), listen.host(), listen.port());
					HttpSettings settings = getHttpSettings(listen, registration.applicationIdentity(), registration.applicationVersion());
					
					server.deployHttp(identity, registration.flow(), settings);
					
					registration.undeploy(version -> server.undeploy(identity, version));
					registration.pause(version -> server.pause(identity, version));
					registration.resume(version -> server.resume(identity, version));
					
					registration.network(listen.port(), settings.isSsl() ? "https" : "http", details -> {
						details.putString("host", listen.host());
					});
				
				}
			});
		}
		
	}
	
	private HttpSettings getHttpSettings(HostAndPort listen, String applicationIdentity, int applicationVersion) {
		String host = listen.host();
		int port = listen.port();
		
		if (port == -1) {
			port = ssl != null ? 443 : 80;
		}
	
		if (ssl != null) {
			return  HttpSettings.https(port, host, Type.HTTP, applicationIdentity, applicationVersion, ssl);
		} else {
			return HttpSettings.http(port, host, Type.HTTP, applicationIdentity, applicationVersion);
		}
	}

}
